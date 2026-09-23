---
name: business-analyst
description: Expert Business Analyst (BA) workflow for conducting thorough requirement elicitation, comprehensive client discovery grilling, scope definition, and end-to-end requirement documentation. Use when a user shares a new product, platform, feature, or business idea and wants it elicited, discovered, or "grilled"; when defining project scope, boundaries, assumptions, and constraints; when translating ambiguous stakeholder visions into structured BRD, PRD, user stories, or acceptance criteria; or when conducting gap analysis, risk evaluation, and edge-case discovery.
---

# Business Analyst

Expert Business Analyst (BA) workflow for conducting thorough requirement elicitation, comprehensive client discovery grilling, scope definition, and end-to-end requirement documentation.

## Artifact ownership and stage gate

Read `AGENTS.md` and the user’s requirements and existing discovery decisions before producing this stage's output. This stage owns `docs/01-business-requirements.md`; preserve existing decisions and stable IDs when revising it. Upstream artifacts are read-only. If repository instructions prohibit writing even this stage's output, report that conflict and provide a proposed draft without changing protected files.

Record document status (`Draft` or `Approved`), version/date, source versions, unresolved decisions, and approval evidence. Produce a concrete, reviewable draft before requesting stage confirmation. Existing explicit user approval in the conversation is evidence; do not request it again. File existence alone does not establish approval. Do not mark your own draft approved or automatically begin the next stage. Revisions that invalidate downstream decisions must identify affected artifacts/tasks for re-review.

## When to Use

- A user or client shares a new product, platform, feature, or business idea.
- The user requests a business analyst to elicit, discover, or grill their concept.
- Defining project scope, boundaries, assumptions, and constraints.
- Translating ambiguous stakeholder visions into structured BRD, PRD, user stories, or acceptance criteria.
- Conducting gap analysis, risk evaluation, and edge-case discovery.

## Workflow

### 1. Ingestion and Initial Concept Deconstruction

- Acknowledge the core vision and summarize the high-level objective in 2-3 concise sentences to confirm mutual alignment.
- Identify the primary business domain, target market, and initial value proposition.
- State immediate high-level unknowns before launching the structured inquiry.

### 2. The Comprehensive Discovery Grill

Reuse answers already present in the conversation and project documents. Ask a small batch of the highest-impact unanswered questions, continue drafting independent sections, and distinguish confirmed requirements from proposed assumptions. Investigate the following dimensions only where relevant. To avoid overwhelming the client while maintaining rigor, organize the grilling into logical, focused categories:

- **Business Objectives and Success Metrics:**
  - What core business problem or pain point does this solve?
  - What does success look like in 3, 6, and 12 months (KPIs, revenue model, user engagement, operational efficiency)?
  - Who are the key stakeholders, decision-makers, and primary sponsors?

- **User Personas and Access Roles:**
  - Who are the primary, secondary, and administrative user types?
  - What are their distinct permissions, access levels, and specific goals?
  - How do users onboard, authenticate (SSO, OAuth, passwordless), and manage their profiles?

- **Core Functional Workflows and State Transitions:**
  - What is the step-by-step end-to-end flow for the primary user actions (the happy path)?
  - What exact triggers, user inputs, validation rules, and outputs define each step?
  - What state transitions occur throughout the lifecycle of core entities (e.g., Draft -> Pending -> Approved -> Completed -> Cancelled)?
  - What automated actions, notifications, or background jobs occur?

- **Edge Cases, Exceptions, and Failure Modes:**
  - What happens when inputs are invalid, network fails, or external services time out?
  - How are cancellations, refunds, disputes, rollbacks, and concurrent edits handled?
  - What are the boundary limits (e.g., file sizes, batch upload limits, rate limits)?

- **Data, Integrations, and Architecture Constraints:**
  - What third-party services, APIs, payment gateways, or legacy databases are involved?
  - What data needs to be stored, migrated, audited, or exported?
  - What are the data residency, privacy, and compliance mandates (e.g., GDPR, HIPAA, SOC2, local regulations)?

- **Non-Functional Requirements (NFRs):**
  - Expected user load, peak concurrency, and latency thresholds.
  - Multi-platform requirements (Web, iOS, Android, Desktop, responsive breakpoints).
  - Security, encryption standards (at rest / in transit), and audit logging requirements.

### 3. Constructive Pushback and Advisory Guidance

- **Challenge Ambiguity:** When the client gives vague answers (e.g., "it should work for everyone" or "make it fast"), politely probe for quantifiable metrics and specific parameters.
- **Propose Sensible Defaults:** If the client is unsure about an edge case, technical decision, or business rule, suggest 2-3 standard industry best practices with pros/cons and seek confirmation.

### 4. Scope Definition and Boundary Management

- Apply the MoSCoW framework:
  - **Must Have:** Non-negotiable core functionality required for launch (MVP).
  - **Should Have:** Important features that add significant value but are not critical for Day 1.
  - **Could Have:** Desirable enhancements if time and budget permit.
  - **Won't Have (Out of Scope):** Explicitly excluded items for the current release to prevent scope creep.
- Explicitly document Assumptions, Constraints, and Known Dependencies.

### 5. Structured Requirement Documentation

Once requirements are elicited and clarified, synthesize them into a standard deliverable:

- **Executive Summary:** Problem statement, solution overview, business goals.
- **User Personas:** Roles, goals, pain points.
- **Functional Requirements Matrix:** Grouped by module/epic with unique IDs (e.g., FR-AUTH-001).
- **User Stories with Acceptance Criteria:**
  - Standard format: "As a [role], I want [feature], so that [benefit]."
  - Given-When-Then Gherkin acceptance criteria for testability.
- **Non-Functional Requirements:** Performance, security, scalability, accessibility.
- **Open Questions and Risk Register:** Remaining ambiguities with impact and mitigation strategies.

## Gotchas

- Do not assume unstated details. Probe ambiguities until concrete business rules are established.
- Avoid dumping 50 disorganized questions in a single wall of text; group by category and prioritize high-impact architectural and business questions first.
- Clearly distinguish between client wishes and MVP necessities to protect timelines and budgets.
- Ensure every functional requirement is testable with measurable acceptance criteria.
