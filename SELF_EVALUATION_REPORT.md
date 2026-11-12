# Self-Evaluation Report

**Project:** Intelligent Payment Fraud Investigation & Dispute Resolution Agent
**Participant:** Dhanappa Jawalkote
**Evaluator roles:** Senior Java AI Solution Architect · BFS Fraud-Domain Reviewer · Secure-Code Reviewer · Test Architect
**Date:** 2026-11-12
**Build state under review:** `mvn test` — 25 tests, 0 failures · app verified live at runtime (intake → evidence → assessment → human review → audit)

---

## 1. Executive Summary

The capstone delivers a genuinely end-to-end, runnable system: every requested
feature exists and is observable at runtime — intake with layered validation,
six deterministic evidence tools, an agent/rules processing seam, policy RAG
with verbatim citations, case-isolated conversation memory, human review
gating, and an append-only audit trail, over both REST and a working Thymeleaf
dashboard. The framework-neutral `FraudCaseProcessor` port is implemented
honestly (not cosmetic), and the rules engine is a real fallback, not a stub.

The dominant weaknesses are **security posture** (no authentication or
authorization anywhere — anyone who can reach the app can approve a dispute)
and **verification of the AI path** (the LLM agent implementation is present
and correct against the Spring AI API, but untested and unexercised in this
environment). Both are understandable in a capstone context and both are
remediable; they are documented below rather than hidden.

**Overall verdict: Strong pass — 80/100.** Production-adjacent design;
deliberate, disclosed gaps rather than silent defects.

| Area | Score |
|---|---|
| 1. Requirements coverage | 9.0 / 10 |
| 2. Architecture & design | 8.5 / 10 |
| 3. Agentic AI & Spring AI usage | 8.0 / 10 |
| 4. BFS fraud-domain correctness | 8.0 / 10 |
| 5. Security | 5.5 / 10 |
| 6. Persistence & data integrity | 8.0 / 10 |
| 7. Testing | 7.5 / 10 |
| 8. Operability & documentation | 8.5 / 10 |
| 9. Code quality | 8.0 / 10 |

---

## 2. Requirements Traceability

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Fraud case intake and validation | Done | `CaseIntakeRequest` bean rules + `FraudCaseService.businessValidate` (duplicate txn, future dates); rejected intakes audited as `INTAKE_REJECTED` |
| 2 | Six deterministic synthetic evidence tools | Done | `evidence/` — velocity, geo/impossible-travel, device reputation, merchant risk, card-testing, account history; SHA-256-seeded, determinism proven by test |
| 3 | AI agent with tool calling | Done (unexercised live) | `AgentEvidenceTools` — 6 tools + `policyLookup` via `@Tool`/`ToolCallbacks` |
| 4 | Banking policy RAG with exact citations | Done | 6 policy docs → `SimpleVectorStore`; citations = docId + section + verbatim excerpt; model cites IDs only |
| 5 | Rules mode as AI fallback | Done | `DelegatingFraudCaseProcessor` — absent chat model *and* any runtime exception → rules; marked `RULES_FALLBACK` |
| 6 | Structured fraud risk recommendations | Done | `FraudAssessment` (score/level/action/factors/rationale/citations); `.entity(AgentVerdict.class)` |
| 7 | Case-isolated conversation memory | Done | `MessageWindowChatMemory` keyed `case-{id}`, mirrored to `chat_transcript` |
| 8 | Human review & approval workflow | Done | `ReviewService` state machine; override/info-request require justification |
| 9 | Append-only audit trail | Done | `@PreUpdate`/`@PreRemove` throw; writes use `REQUIRES_NEW`; enforced by test |
| 10 | REST API + responsive dashboard | Done | `/api/**` + Bootstrap Thymeleaf; all pages verified HTTP 200 |

**Gap:** the AI path (req. 3) is implemented but could not be exercised live
in this environment (no API key) — it is covered only by code review, not by
an executed test.

---

## 3. Architecture & Design Review

### Strengths

- **Real hexagonal seam.** `domain/` contains zero Spring/JPA/Spring-AI
  imports. `FraudCaseProcessor` and `EvidenceTool` are genuine ports; both
  implementations are swappable at runtime, and the same `EvidenceTool`
  beans serve rules mode and LLM tool-calling — evidence cannot diverge
  between modes.
- **Fallback is structural, not hopeful.** AI absence is detected at bean
  level; AI failure is caught per-call; the fallback result is explicitly
  labelled `RULES_FALLBACK` and audited — an examiner can always tell which
  engine answered.
- **Citations are unforgeable.** The model returns `citedPolicyIds`; the
  service resolves them against retrieved documents. Excerpts in the record
  are verbatim from the corpus, not model output.
- **Memory isolation is correct.** `conversationId = "case-" + id` means one
  case's context can never bleed into another's; the durable transcript
  mirror is a nice auditability touch.

### Findings

| Sev | Finding | Location | Recommendation |
|---|---|---|---|
| LOW | `ChatClient` rebuilt inside every `assess()` call | `SpringAiFraudCaseProcessor.assess` | Build once lazily and cache; per-call construction is correct but wasteful |
| LOW | `mergedDetails` flattens all tool details into one map — silent overwrite on future key collisions | `RulesFraudCaseProcessor` | Namespace keys per tool (`toolName.flag`) |
| LOW | Dead branch in `businessValidate` (empty `if` with only a comment) | `FraudCaseService` | Remove or implement the check |
| LOW | Fully-qualified `CaseNotFoundException` inline in `getCase` | `FraudCaseService` | Import normally for consistency |
| NIT | Unused `import java.util.Comparator` | `DashboardController` | Remove |

### Design risks to acknowledge

- **Synchronous investigation.** `intakeCase` runs evidence + assessment in
  the request thread. With a real LLM (5–30s) this couples intake latency to
  model latency. Correct for capstone scope; production needs `@Async`/queue
  + `INVESTIGATING` status polling.
- **In-memory vector store & chat memory.** Restart loses both (transcript
  and citations persist, so no data-integrity loss). Documented limitation;
  pgvector + `JdbcChatMemoryRepository` are the named upgrades.
- **`@PostConstruct` corpus indexing** blocks startup; fine for 18 sections,
  should move to a runner/reindex endpoint at scale.

---

## 4. BFS Fraud-Domain Review

### Correct / credible

- **Reg E content is accurate:** $50/$500 liability tiers, 2-business-day and
  60-day windows, 10-business-day provisional credit, 45/90-day investigation
  clocks (POL-100 §1–2 matches 12 CFR 1005.6/1005.11).
- **Escalation logic is policy-driven:** impossible travel, card-testing,
  watchlisted merchant, ATO signal force ≥70 → escalate (POL-515 §2) —
  implemented as overrides, not just score massaging.
- **Sensible scoring semantics:** score = strength of corroboration that the
  charge was fraudulent; low → deny (POL-620 §3 first-party-fraud logic),
  mid → refund, high → escalate. Serial-disputer carve-out returns
  REQUEST_INFORMATION instead of outright denial (POL-620 §2) — a subtle,
  correct policy interaction.
- **PCI-conscious design:** stores `cardFingerprint`, never PAN/CVV — keeps
  the system out of PCI-DSS cardholder-data scope.
- **Human-in-the-loop is mandatory** per POL-515 §1 — the agent can never
  resolve a case alone. This is the single most important BFS control and it
  is implemented in the state machine, not just claimed.

### Gaps / simplifications

| Sev | Gap | Recommendation |
|---|---|---|
| MED | No network (Visa/Mastercard) chargeback reason codes or representment flow | Add `networkReasonCode` to case + assessment; model the representment state |
| MED | Provisional credit mentioned in policy but not modelled as a case attribute/action | Add `provisionalCreditIssued` flag + deadline tracking |
| LOW | `DisputeReason` omits non-fraud disputes (service not received, not as described) | Out of scope for a *fraud* agent, but note the boundary explicitly |
| LOW | `occurredAt` not bounded below (a 10-year-old dispute validates) | Add staleness check vs. Reg E/network timeframes |
| INFO | `minutesSincePreviousAuth` in geo tool is synthetic-random rather than derived | Acceptable for synthetic evidence; document determinism rationale |

---

## 5. Secure-Code Review

### Positive controls verified

- **XSS:** every template binding uses `th:text` (auto-escaped); zero `th:utext` in the codebase — confirmed by grep.
- **Injection:** all DB access via JPA repositories/converters — no string-concatenated queries; tool seeds via `MessageDigest`, not `String.hashCode`.
- **Secrets:** none in source or config; `OPENAI_API_KEY` via env var; placeholder default is inert.
- **Immutability of audit:** enforced at ORM level with tests proving update/delete refusal.
- **Audit-before-mutation:** intake, evidence, assessment, decision, resolution all audited before/around state change.

### Findings

| Sev | Finding | Evidence | Recommendation |
|---|---|---|---|
| **HIGH** | **No authentication or authorization anywhere** — any caller can file, investigate, and *approve* disputes | no `spring-security` dependency; all endpoints open | Add Spring Security: at minimum HTTP-basic + role `REVIEWER` on `/api/review/**` and the review form; OIDC in production. This is the one finding that would block any real deployment |
| MED | H2 console enabled by default (`/h2-console`) with no auth | `application.yml` | Restrict to a `local`/`dev` profile only |
| MED | No generic `@ExceptionHandler(Exception.class)` — unmapped exceptions (e.g. `DataIntegrityViolationException` on a duplicate-txn race) surface as uncontrolled 500s | `ApiExceptionHandler` | Add catch-all handler returning generic body + full logging server-side |
| MED | No rate limiting / abuse control on intake | — | Bucket4j filter or gateway policy; intake triggers AI spend |
| LOW | `customerNarrative` free-text may carry PII, stored in cleartext | `FraudCaseEntity` | Document PII policy; consider field-level encryption/masking |
| LOW | Append-only is app-level only — a compromised app could `UPDATE`/`DELETE` via raw SQL | `AuditEventEntity` | DB-level enforcement: revoke UPDATE/DELETE grants on `audit_event`, or a blocking trigger |
| INFO | No CORS config — effectively deny-all cross-origin, which is the safe default for this app | — | Explicit policy if a SPA is ever added |

**Security score context:** for a capstone the absence of auth is common, but
as a BFS reviewer I must score it as the material gap it is — reviewer
identity (`reviewerId`) is currently a self-declared string, which undermines
the human-approval control's evidential value.

---

## 6. Persistence & Data Integrity

- **Good:** unique `transaction_id`, IDENTITY PKs, JSON attribute converters
  keep evidence details and citations portable across H2/PostgreSQL,
  `@PrePersist`/`@PreUpdate` timestamps, audit writes isolated via
  `REQUIRES_NEW` (audit survives business rollback).
- **Gaps:**
  - No DB indexes on `case_id` foreign keys across `evidence_record`,
    `assessment_record`, `review_decision`, `audit_event`, `chat_transcript`
    — every detail view does a sequential scan per table (MED, trivially
    fixable via `@Table(indexes=…)`).
  - `fraud_case.status`/state transitions are app-enforced only; a stray
    `UPDATE` could skip the state machine (LOW).
  - Duplicate-txn race: `existsByTransactionId` then insert is a check-then-act;
    concurrent intakes could still hit the unique constraint → unmapped 500 (LOW).

---

## 7. Test Architecture Review

**Coverage:** 25 tests, all green.

| Suite | What it proves |
|---|---|
| `EvidenceToolDeterminismTest` (4) | all six tools deterministic + bounded risk |
| `RulesFraudCaseProcessorTest` (5) | thresholds, all four escalation flags, serial-disputer rule, determinism |
| `PolicyRagServiceTest` (4) | corpus indexed, topical queries hit the right doc/section, case-driven retrieval cited |
| `FraudCaseWorkflowTest` (6) | full pipeline to PENDING_REVIEW, intake rejections, approve/override/info-request state machine |
| `AppendOnlyAuditTest` (2) | update and delete both refused |
| `FraudCaseApiTest` (4) | REST E2E, 400 validation, policy search, 404 |

### Gaps (honest)

| Sev | Missing coverage | Suggested test |
|---|---|---|
| HIGH | **AI agent path: zero tests** — `SpringAiFraudCaseProcessor`, `AgentEvidenceTools`, structured-verdict mapping, and the `RULES_FALLBACK` transition are all unexercised | Mock `ChatModel`/`ChatClient.Builder` to return a fixed `AgentVerdict`; assert processor=`AI_AGENT`, citations resolved, and a throwing model yields `RULES_FALLBACK` |
| MED | No direct test of `reinvestigate` HTTP path or its 409 on resolved cases (service-level covered) | MockMvc: POST reinvestigate on resolved → 409 |
| MED | No test that evidence `details` JSON round-trips through the converter | `@DataJpaTest` on `EvidenceRecordEntity` |
| LOW | No property-based/boundary tests (amount=0.01, score exactly 35/70 thresholds) | Parameterized test on threshold boundaries |
| NIT | `differentFactsUsuallyProduceDifferentEvidence` wording ("usually") belies the fact it is fully deterministic | Rename to reflect determinism |

**Assessment:** the suite is honest — it tests behavior, not mocks — and the
weakest area is the same as the system's weakest area (the unexercised AI
path). Adding the two AI-path tests above is the highest-value test work
remaining.

---

## 8. Operability & Documentation

- README covers architecture, feature map, run modes, API reference, and test
  instructions — accurate against the actual code.
- Zero-setup default (H2) + `postgres` profile + `docker-compose.yml` healthcheck.
- Demo seeder runs real intake pipeline (not fixture inserts) — cases on the
  dashboard are genuine end-to-end artifacts; disable flag present.
- Actuator health exposed; logging is disciplined (no secrets, debug-only
  analysis dump).
- **Minor:** `spring.jpa.open-in-view=false` set in main profile but the test
  profile misses it (warning observed in test logs); app default port 8080
  was in use on this machine — documented `PORT` override works.

---

## 9. Prioritized Remediation Plan

| # | Action | Effort | Closes |
|---|---|---|---|
| 1 | Add Spring Security (basic auth, `REVIEWER` role on review endpoints) | M | HIGH security finding |
| 2 | Add AI-path tests: fixed `ChatModel` → verdict mapping + forced failure → `RULES_FALLBACK` | S | HIGH test gap |
| 3 | Generic `ExceptionHandler(Exception)` + map `DataIntegrityViolationException` → 409 | S | uncontrolled 500s |
| 4 | Add `case_id` indexes on the five child tables | S | persistence perf |
| 5 | Scope H2 console to a `local` profile only | S | MED security finding |
| 6 | Async investigation (`@Async` + status polling) before real LLM use | M | latency coupling |
| 7 | pgvector + `JdbcChatMemoryRepository` for restart-safe RAG/memory | M | durability |
| 8 | Cache the built `ChatClient`; namespace merged evidence details | S | design nits |
| 9 | Chargeback reason codes + provisional-credit fields | M | BFS depth |
| 10 | DB-level append-only (revoked grants or trigger) | S | defense-in-depth |

---

## 10. Final Assessment

**Total: 80 / 100 — Strong pass.**

This is a well-architected capstone that does what it claims: every headline
feature is present, demonstrable at runtime, and covered by tests that verify
behavior rather than implementation. The framework-neutral processor port,
the unforgeable citation mechanism, the mandatory human-decision state
machine, and the enforced append-only audit trail show real BFS instincts —
the system is built so the AI *advises* and humans *decide*, with evidence for
every step.

What separates this from production is exactly two things, both disclosed and
remediable: **there is no identity on the human making the decision** (no
auth), and **the AI half of the dual-engine design is unverified by test**.
Close those and the remaining items are hardening, not rework.

---

*Report generated under standard senior-architect / BFS-domain / secure-code /
test-architecture rubric, in lieu of the referenced
`Devin_Self_Evaluation_Prompt_Fraud_Capstone.md` (file not present on the
evaluation host).*
