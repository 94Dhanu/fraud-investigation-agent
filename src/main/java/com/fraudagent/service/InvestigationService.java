package com.fraudagent.service;

import com.fraudagent.ai.SpringAiFraudCaseProcessor;
import com.fraudagent.domain.exception.InvalidStateTransitionException;
import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.CaseStatus;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.FraudAssessment;
import com.fraudagent.domain.port.EvidenceTool;
import com.fraudagent.domain.port.FraudCaseProcessor;
import com.fraudagent.persistence.entity.AssessmentRecordEntity;
import com.fraudagent.persistence.entity.ChatTranscriptEntity;
import com.fraudagent.persistence.entity.EvidenceRecordEntity;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.repository.AssessmentRecordRepository;
import com.fraudagent.persistence.repository.ChatTranscriptRepository;
import com.fraudagent.persistence.repository.EvidenceRecordRepository;
import com.fraudagent.persistence.repository.FraudCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Investigation orchestration:
 * <ol>
 *   <li>run the six deterministic evidence tools and persist the records</li>
 *   <li>invoke the {@link FraudCaseProcessor} (AI agent or rules fallback)</li>
 *   <li>persist the structured assessment with its policy citations</li>
 *   <li>mirror the agent conversation into the durable transcript</li>
 *   <li>move the case to PENDING_REVIEW and audit every step</li>
 * </ol>
 */
@Service
public class InvestigationService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);

    private final FraudCaseRepository caseRepository;
    private final EvidenceRecordRepository evidenceRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final ChatTranscriptRepository transcriptRepository;
    private final List<EvidenceTool> evidenceTools;
    private final FraudCaseProcessor fraudCaseProcessor; // primary: AI or rules fallback
    private final AuditService auditService;
    private final CaseFactsProvider factsProvider;
    private final ChatMemory chatMemory;

    public InvestigationService(FraudCaseRepository caseRepository,
                                EvidenceRecordRepository evidenceRepository,
                                AssessmentRecordRepository assessmentRepository,
                                ChatTranscriptRepository transcriptRepository,
                                List<EvidenceTool> evidenceTools,
                                FraudCaseProcessor fraudCaseProcessor,
                                AuditService auditService,
                                CaseFactsProvider factsProvider,
                                ChatMemory chatMemory) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.assessmentRepository = assessmentRepository;
        this.transcriptRepository = transcriptRepository;
        this.evidenceTools = evidenceTools;
        this.fraudCaseProcessor = fraudCaseProcessor;
        this.auditService = auditService;
        this.factsProvider = factsProvider;
        this.chatMemory = chatMemory;
    }

    @Transactional
    public FraudAssessment investigate(Long caseId) {
        FraudCaseEntity entity = factsProvider.load(caseId);
        if (entity.getStatus() == CaseStatus.RESOLVED || entity.getStatus() == CaseStatus.REJECTED) {
            throw new InvalidStateTransitionException(
                    "Case " + caseId + " is " + entity.getStatus() + " and cannot be reinvestigated");
        }

        entity.setStatus(CaseStatus.INVESTIGATING);
        caseRepository.save(entity);
        auditService.record(caseId, "fraud-agent", AuditService.INVESTIGATION_STARTED,
                "Investigation started — running " + evidenceTools.size() + " evidence tools");

        CaseFacts facts = CaseFactsProvider.toFacts(entity);
        List<EvidenceItem> evidence = evidenceTools.stream()
                .map(tool -> tool.run(facts))
                .toList();
        evidence.forEach(item -> evidenceRepository.save(toEntity(caseId, item)));
        auditService.record(caseId, "fraud-agent", AuditService.EVIDENCE_COLLECTED,
                evidence.size() + " evidence records collected: "
                        + evidence.stream().map(EvidenceItem::toolName).sorted().toList());

        FraudAssessment assessment = fraudCaseProcessor.assess(facts, evidence);
        assessmentRepository.save(toEntity(caseId, assessment));
        persistAgentTranscript(caseId);
        auditService.record(caseId, "fraud-agent", AuditService.ASSESSMENT_RECORDED,
                "Assessment by " + assessment.processorUsed() + ": risk " + assessment.riskScore()
                        + "/100 (" + assessment.riskLevel() + ") → " + assessment.recommendedAction());

        FraudCaseEntity updated = factsProvider.load(caseId);
        updated.setStatus(CaseStatus.PENDING_REVIEW);
        caseRepository.save(updated);
        return assessment;
    }

    private void persistAgentTranscript(Long caseId) {
        String conversationId = SpringAiFraudCaseProcessor.conversationIdFor(caseId);
        List<Message> messages;
        try {
            messages = chatMemory.get(conversationId);
        } catch (Exception e) {
            return; // rules mode — nothing persisted
        }
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (Message message : messages) {
            ChatTranscriptEntity row = new ChatTranscriptEntity();
            row.setCaseId(caseId);
            row.setRole(message.getMessageType().name());
            row.setContent(message.getText());
            transcriptRepository.save(row);
        }
    }

    private EvidenceRecordEntity toEntity(Long caseId, EvidenceItem item) {
        EvidenceRecordEntity e = new EvidenceRecordEntity();
        e.setCaseId(caseId);
        e.setToolName(item.toolName());
        e.setSummary(item.summary());
        e.setRiskContribution(item.riskContribution());
        e.setDetails(item.details());
        return e;
    }

    private AssessmentRecordEntity toEntity(Long caseId, FraudAssessment a) {
        AssessmentRecordEntity e = new AssessmentRecordEntity();
        e.setCaseId(caseId);
        e.setRiskScore(a.riskScore());
        e.setRiskLevel(a.riskLevel());
        e.setRecommendedAction(a.recommendedAction());
        e.setKeyFactors(a.keyFactors());
        e.setRationale(a.rationale());
        e.setPolicyCitations(a.policyCitations());
        e.setProcessorUsed(a.processorUsed());
        return e;
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<EvidenceRecordEntity> evidenceFor(Long caseId) {
        return evidenceRepository.findByCaseIdOrderByIdAsc(caseId);
    }

    @Transactional(readOnly = true)
    public List<AssessmentRecordEntity> assessmentsFor(Long caseId) {
        return assessmentRepository.findByCaseIdOrderByIdDesc(caseId);
    }

    @Transactional(readOnly = true)
    public List<ChatTranscriptEntity> transcriptFor(Long caseId) {
        return transcriptRepository.findByCaseIdOrderByIdAsc(caseId);
    }
}
