# Intelligent Payment Fraud Investigation & Dispute Resolution Agent

A complete, runnable Spring Boot 3 application that investigates card-dispute
fraud cases end-to-end: intake → deterministic evidence collection → AI-agent
(or rules-engine) risk recommendation with **exact policy citations** →
mandatory human review → append-only audit trail.

## Tech stack

Java 17 · Spring Boot 3.5 · Maven · Spring AI 1.0 (tool calling, structured
output, ChatMemory, VectorStore) · Spring Data JPA · PostgreSQL (prod) / H2
(zero-setup default) · Thymeleaf + Bootstrap 5 · JUnit 5

## Architecture (clean, framework-neutral core)

```
api/            REST controllers + Thymeleaf dashboard + DTOs
service/        Intake/validation · InvestigationService · ReviewService · AuditService
domain/
  model/        CaseFacts, FraudAssessment, PolicyCitation, enums
  port/         FraudCaseProcessor  ← framework-neutral interface
                EvidenceTool        ← deterministic tool contract
evidence/       6 synthetic evidence tools (deterministic, seeded by facts hash)
ai/
  RulesFraudCaseProcessor      — weighted scoring + policy overrides (default)
  SpringAiFraudCaseProcessor   — LLM agent: tool calling + per-case memory + RAG
  DelegatingFraudCaseProcessor — @Primary router; AI failure ⇒ RULES_FALLBACK
  PolicyRagService             — policy corpus → SimpleVectorStore → exact citations
  LocalHashingEmbeddingModel   — deterministic offline embeddings
persistence/    JPA entities incl. append-only AuditEvent (update/delete blocked)
resources/policies/  POL-100…POL-620 banking policy corpus (markdown)
```

Everything in `domain/` is plain Java — no Spring, no JPA, no Spring AI types.
`FraudCaseProcessor` is the seam: `RulesFraudCaseProcessor` and
`SpringAiFraudCaseProcessor` are interchangeable implementations selected at
runtime.

## Feature map

| Requirement | Where |
|---|---|
| Intake + validation | `CaseIntakeRequest` bean rules + `FraudCaseService.businessValidate` (duplicate txn, future dates); rejected intakes are audited |
| Six deterministic evidence tools | `evidence/` — velocity, geo/impossible-travel, device reputation, merchant risk, card-testing, account history. Same facts ⇒ same evidence (SHA-256-seeded) |
| AI agent + tool calling | `AgentEvidenceTools` exposes the same 6 tools + `policyLookup` to the LLM via `@Tool`/`ToolCallbacks` |
| Banking policy RAG + exact citations | `PolicyRagService` chunks 6 policy docs into `SimpleVectorStore`; citations carry docId + section + verbatim excerpt; model cites IDs, never text |
| Rules mode as AI fallback | `DelegatingFraudCaseProcessor`: no chat model OR any AI exception ⇒ rules engine, marked `RULES_FALLBACK` |
| Structured recommendations | `FraudAssessment` (score, level, action, factors, rationale, citations); LLM output via `.entity(AgentVerdict.class)` |
| Case-isolated memory | `MessageWindowChatMemory` keyed `case-{id}`; mirrored to `chat_transcript` for audit |
| Human review workflow | `ReviewService`: approve / override (justification required) / request-info; only PENDING_REVIEW cases decidable |
| Append-only audit trail | `AuditEventEntity` `@PreUpdate`/`@PreRemove` throw; audit writes use `REQUIRES_NEW` |
| REST + dashboard | `/api/**` JSON API; Thymeleaf+Bootstrap pages at `/dashboard` |

## Run it

```bash
mvn spring-boot:run
```

Dashboard: http://localhost:8080/dashboard · REST: http://localhost:8080/api/cases
· H2 console: http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:fraudagent`)

Four demo cases are seeded automatically (`fraud.seed-demo-data=false` to disable).

### PostgreSQL mode

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### AI agent mode (LLM)

```bash
export OPENAI_API_KEY=sk-…
export FRAUD_AI_MODEL=openai FRAUD_AGENT_MODE=ai
mvn spring-boot:run
```

The agent calls the six evidence tools, retrieves policy sections, reasons in
per-case memory, and emits a structured verdict with real citations. If the API
key is missing or the call fails, the same case is assessed by the rules engine
and tagged `RULES_FALLBACK` — nothing breaks.

## REST API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/cases` | Intake + validate + investigate (201) |
| GET | `/api/cases` | List cases |
| GET | `/api/cases/{id}` | Full dossier (evidence, assessment, citations, transcript, audit) |
| POST | `/api/cases/{id}/reinvestigate` | Re-run pipeline |
| GET | `/api/cases/{id}/audit` | Audit trail |
| GET | `/api/review/queue` | Pending-review queue |
| POST | `/api/review/{id}/decision` | Human decision |
| GET | `/api/policies/search?q=` | Semantic policy search with citations |

Example:

```bash
curl -X POST localhost:8080/api/cases -H 'Content-Type: application/json' -d '{
  "transactionId":"TXN-99120","cardFingerprint":"card_fp_e81a20c4",
  "accountId":"ACC-101110","merchantId":"MERCH-3301","merchantCategory":"CRYPTO",
  "amount":750.00,"currency":"USD","occurredAt":"2026-11-11T22:15:00Z",
  "customerCountry":"US","merchantCountry":"NG","channel":"ECOMMERCE",
  "disputeReason":"UNAUTHORIZED_TRANSACTION",
  "customerNarrative":"I never made this purchase."
}'

curl -X POST localhost:8080/api/review/1/decision -H 'Content-Type: application/json' -d '{
  "reviewerId":"analyst.jane","decision":"APPROVE_RECOMMENDATION"
}'
```

## Tests

```bash
mvn test
```

Covers: evidence-tool determinism, rules scoring + policy escalation overrides,
RAG citation relevance, intake validation, review workflow state machine,
append-only audit enforcement, and a MockMvc API smoke test.
