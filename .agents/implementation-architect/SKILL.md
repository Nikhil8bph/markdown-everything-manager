---
name: implementation-architect
description: Translates Business Requirements (BRD), Solution Architecture Plans (Application Development Plan), and API Contracts into a structured implementation strategy, Backlog Items (BLIs), Tasks, Activities, Subtasks, and sprint execution phases. Aligned with the approved Angular, Spring Boot, or FastAPI full-stack architecture standards. Use when the user presents BRDs, architecture plans, OpenAPI/AsyncAPI contracts, or system specifications and asks to break them down into backlog items, development tasks, subtasks, work breakdown structures (WBS), sprint plans, implementation roadmaps, or engineering tickets.
---

# Implementation Architect

## Artifact ownership and stage gate

Read `AGENTS.md` and the user-confirmed documents from stages 1–3 and relevant machine-readable contracts before producing this stage's output. This stage owns `docs/04-implementation-strategy.md`; preserve existing decisions and stable IDs when revising it. Upstream artifacts are read-only. If repository instructions prohibit writing even this stage's output, report that conflict and provide a proposed draft without changing protected files.

Record document status (`Draft` or `Approved`), version/date, source versions, unresolved decisions, and approval evidence. Produce a concrete, reviewable draft before requesting stage confirmation. Existing explicit user approval in the conversation is evidence; do not request it again. File existence alone does not establish approval. Do not mark your own draft approved or automatically begin the next stage. Revisions that invalidate downstream decisions must identify affected artifacts/tasks for re-review.

## Summary

Translates business requirement documents (BRD), Application Development Planning Documents (ADP), API Contract & Integration Specification Documents, and machine-readable OpenAPI/AsyncAPI schemas into an end-to-end implementation strategy. Structures delivery into hierarchical Backlog Items (BLIs), Tasks, Activities, and Subtasks, and organizes them into milestone phases, fully aligned with the approved Angular, Spring Boot, or FastAPI architectural standards.

## Prerequisites

This skill is the implementation-planning bridge between architecture and code. It requires all three upstream artifacts to already exist and be confirmed with the user:

- **Business Requirement Document (BRD)** - from the `business-analyst` skill.
- **Application Development Planning Document (ADP)** - from the `solution-architect-app-planning` skill.
- **API Contract & Integration Specification Document** - from the `solution-architect-api-contracts` skill (OpenAPI/AsyncAPI specs, DTOs, event schemas).

If any of these are missing or incomplete, ask the user for them or recommend running the appropriate earlier-stage skill first. Do not invent business rules, architecture decisions, or API shapes that were not established in those documents - traceability back to them is mandatory for every backlog item produced.

## When to Use

- When receiving approved BRDs, Application Development Planning Documents, API contracts, or OpenAPI/AsyncAPI specifications from Business Analysts or Solution Architects.
- When breaking down complex application requirements into a structured Work Breakdown Structure (WBS), Backlog Items (BLIs), Tasks, Activities, and Subtasks.
- When planning execution sprints, release milestones, dependency graphs, and critical path analysis for full-stack enterprise systems.
- When defining exact developer-level subtasks, including Spring Boot 4-layer backend components (Controller, Service, Facade, Repository, DTOs, MapStruct, Security, JPA Specs, Hibernate Filters) and Angular frontend artifacts (Standalone components, Signals, OnPush, Interceptors, Guards, Feature Toggles, Routes, CLI generation commands).
- When establishing Definition of Done (DoD), technical prerequisites, and testing criteria across all engineering disciplines.
- This skill produces the plan; it hands off to `spring-boot-enterprise-architect`, `python-fastapi-enterprise-architect`, `angular-enterprise-architect`, and `infrastructure-agent` for actual code implementation of each subtask.

## Task ownership and execution handoff

Write a centralized status table with Task ID, owner/discipline, dependencies, status, updated date, and evidence/blocker notes. Use `Not Started`, `In Progress`, `Blocked`, and `Completed`. Make each task small enough for one agent run and identify affected files/resources so separate runs can avoid conflicting writes. For cross-disciplinary tasks, distinguish partial evidence from completion of the entire task. Planning does not mark implementation tasks completed.

Use only the selected stack and integrations. The examples below are conditional, not a requirement to introduce every store, provider, field, or feature. Plan separate UX handoff dependencies for UI tasks; `docs/DESIGN.md` must cover the scope with status `Ready for Angular`. Backend-only tasks do not require a UX artifact.

For FastAPI tasks, specify the approved Python package path, `APIRouter`/Pydantic models, dependency providers, service boundaries, persistence/transaction handling, migrations, auth policy, and pytest/API checks. Do not translate Java facades, annotations, or Maven modules mechanically into Python. Map each task to `python-fastapi-enterprise-architect` where appropriate.

## Work Breakdown Hierarchy

Structure all deliverables using a strict 5-tier work breakdown hierarchy:

1. **Phase / Milestone (Release Stage)**: Logical release gate or delivery increment (e.g., Phase 0: Foundation and Ingress, Phase 1: Identity and Workspaces, Phase 2: Core Collaboration and Database Engine).
2. **Backlog Item (BLI / Epic / Feature Group)**: Domain-level functional capability mapped to business requirements (e.g., `BLI-AUTH`: Identity and Multi-Session Management, `BLI-PAGE`: Block-Based Workspace Content Engine).
3. **Task (Story / Feature Deliverable)**: Implementable unit of business value with clear acceptance criteria and interface boundaries (e.g., `TASK-AUTH-001`: User Registration, Email Verification and Token Lifecycle).
4. **Activity (Discipline / Work Stream)**: Grouping of work by technical discipline:
   - Database and Persistence (SQL/NoSQL Schemas, Migrations, Indexes, Caching)
   - Backend Engineering (Spring Boot 4-Layer Implementation, Security, Events)
   - API Contracts and Messaging (OpenAPI routes, AsyncAPI channels, CloudEvents)
   - Frontend Engineering (Angular UI Components, Signals, Services, Guards)
   - Infrastructure and DevTooling (Gateway, Eureka, Kafka, Docker, MailHog/SMSHog)
   - QA, Security and Verification (Unit, Integration, Contract, Performance Tests)
5. **Subtask (Atomic Developer Action)**: Concrete, code-level execution step specifying exact classes, interfaces, Angular CLI commands, configs, or test cases.

## Step-by-Step Implementation Planning Workflow

### Step 1: Specification Ingestion and Domain Mapping

- Ingest input documents:
  - **BRD**: Extract functional requirements (FR matrix), user personas, acceptance criteria, and NFRs.
  - **Application Development Planning Document (ADP)**: Identify microservice/module boundaries, database technologies (PostgreSQL, MongoDB, Redis), messaging topology (Kafka), search (OpenSearch), object storage (MinIO), and resiliency patterns.
  - **API Contract & Integration Specification Document** (OpenAPI 3.1.0 YAMLs, AsyncAPI 3.0.0 channels, CloudEvents schemas): Map routes, DTO shapes, WebSocket protocols, event topics, error codes, and headers.
- Build a domain mapping matrix connecting BRD functional IDs (e.g., `FR-OPS-001`) to architecture services/modules, OpenAPI endpoints, and Kafka topics.

### Step 2: Milestone and Sprint Phase Structuring

Group tasks into chronological delivery phases respecting technical dependencies. Adapt phase names/content to the actual project scope from the ADP, but a typical sequence is:

- **Phase 0: Infrastructure Foundation and Common Libraries** - Maven-only Spring Boot setup or the approved Python packaging setup, common DTOs/envelopes, Eureka service discovery, Spring Cloud Gateway ingress, local Docker stack (PostgreSQL, Mongo, Redis, Kafka, OpenSearch, MinIO, MailHog, SMSHog), and Angular base scaffolding.
- **Phase 1: Identity, RBAC and Multi-Session Security** - Spring Security JWT filter, OAuth2, Redis multi-session eviction, password reset, MailHog/SMSHog verification, Angular auth guards/interceptors, and login/register UI.
- **Phase 2: Core Domain Entities and Persistence** - relational schemas (Liquibase/Flyway), MongoDB collections, Spring Boot 4-layer CRUD (Controller, Service, Facade, Repository), JPA specifications, Hibernate soft-delete/active filters, MapStruct mappers, and Angular feature modules/smart-dumb components.
- **Phase 3: Real-Time Collaboration, Messaging and Search** - WebSocket/real-time sync gateway, Kafka producer/consumer pipelines with Transactional Outbox and DLQ, OpenSearch indexing, Angular signal-based real-time state, and editor/UI integration.
- **Phase 4: Integrations, Notifications and Portability** - payment orders/webhooks (e.g., Razorpay), FCM/APNs notification worker, async import/export batch jobs, outbound webhook dispatcher, and developer token management.
- **Phase 5: Hardening, Rate Limiting and Production Readiness** - Bucket4j Redis rate limiting, circuit breakers, contract linting (Redocly), performance benchmarks, and end-to-end regression test suites.

### Step 3: Backlog Item (BLI) and Task Decomposition

For each Backlog Item and Task, define:

- **Task ID and Title**: e.g., `TASK-PAGE-002`: Real-Time Block Operation Batching and Sync.
- **Mapped Requirements**: FR IDs, ADR references, OpenAPI endpoint paths, AsyncAPI topic names.
- **Description and Business Value**: Concise summary of purpose and user impact.
- **Given-When-Then Acceptance Criteria**: Verifiable test scenarios.
- **Technical Pre-conditions and Dependencies**: Required prior tasks or infrastructure components.

### Step 4: Activity and Granular Subtask Generation

Under each Task, decompose into actionable subtasks across disciplines:

**1. Database and Persistence Subtasks**
- Define SQL DDL migration scripts (Flyway/Liquibase) with primary keys (UUID), foreign keys, indexes, and audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `is_deleted`).
- Define MongoDB document schemas and indexes.
- Define Redis cache keys, data structures (Hash, String, Set), and explicit TTL policies.

**2. Spring Boot Backend Subtasks (4-Layer Pattern)**
- **Repository Layer**: Create a Spring Data JPA repository extending `JpaRepository` and `JpaSpecificationExecutor`. Add custom queries and dynamic `Specification<T>` methods.
- **Facade Layer**: Create a facade interface and implementation to handle multi-repository aggregation, tenant/user context resolution, and MapStruct mapping between facade DTOs and database entities.
- **Service Layer**: Create a service interface and implementation for core business logic, validation, transactional boundaries (`@Transactional`), and MapStruct mapping between API DTOs and facade DTOs.
- **Controller Layer**: Create a REST controller with versioned mapping (`/api/v1/...`), OpenAPI annotations (`@Operation`, `@ApiResponse`), input validation (`@Valid`), and the project's standardized response envelope.
- **Security and Authorization**: Apply method security (`@PreAuthorize`), custom role precedence checks, and Hibernate filters (`@FilterDef`, `@Filter`).
- **Event Messaging**: Implement a Kafka producer with the Transactional Outbox pattern, a consumer with idempotent deduplication via Redis, and DLQ error handling.

**3. Angular Frontend Subtasks (Standalone and Signals)**
- Scaffolding commands, e.g.:
  ```bash
  ng g c features/<feature>/pages/<page> --standalone --change-detection=OnPush --style=scss --inline-template=false --inline-style=false --skip-tests=false
  ng g c features/<feature>/components/<comp> --standalone --change-detection=OnPush --style=scss --inline-template=false --inline-style=false --skip-tests=false
  ng g s features/<feature>/services/<service>
  ng g interface features/<feature>/models/<model>.model
  ng g guard core/guards/<guard> --functional
  ng g interceptor core/interceptors/<interceptor> --functional
  ```
- **Environment and Endpoints**: Add route constants to the endpoint constants file and feature flags to the environment files.
- **State and Reactivity**: Define signals, computed values, effect triggers, and RxJS pipelines with `takeUntilDestroyed()`.
- **UI Components and Control Flow**: Build standalone templates with modern control flow (`@if`, `@for`, `@defer`), `OnPush` change detection, and signal inputs/outputs.
- **Guards and Interceptors**: Implement functional route guards and HTTP interceptors.

**4. Testing and Verification Subtasks**
- Unit tests: JUnit 5 + Mockito for services and facades; Jasmine/Jest for Angular components and services.
- Integration tests: `@SpringBootTest` with Testcontainers (PostgreSQL, Kafka, Redis, OpenSearch).
- Contract verification: Redocly CLI linting and OpenAPI/AsyncAPI contract validation.

## Deliverable Output Format

Structure the output implementation document as follows:

1. **Executive Strategy and Architecture Alignment**: Summary of scope, tech stack, architecture boundaries, and overall delivery plan.
2. **Delivery Phase and Milestone Roadmap**: Chronological phase breakdown with estimated duration, prerequisites, and milestone deliverables.
3. **Traceability Matrix**: Mapping table connecting BRD FR IDs -> Architecture Services/Modules -> OpenAPI/AsyncAPI Specs -> BLI/Task IDs.
4. **Detailed Work Breakdown Structure**:
   - For each BLI: overview, target persona, MoSCoW priority, and list of Tasks.
   - For each Task: header (ID, title, category, dependencies, target endpoints/topics), user story and acceptance criteria (Gherkin format), discipline activities (Database, Backend, Frontend, Testing), and concrete developer subtasks with code signatures, CLI commands, and schema definitions.
5. **Cross-Cutting Implementation Standards**: Security, multi-session management, Hibernate filters, MapStruct rules, feature toggle conventions, and error handling taxonomy.
6. **Definition of Done (DoD) Checklist**: Verification checklist required before marking any Task or BLI complete.

## Gotchas and Implementation Pitfalls

- Do not skip the Facade layer in Spring Boot. Services must never directly inject or call Repositories.
- Do not use constructor injection in modern Angular. Prefer `inject(ServiceName)` and standalone primitives.
- Do not write raw literal URLs in Angular services. Always reference endpoint constants derived from the environment configuration.
- Do not forget soft-delete and active Hibernate filters. Ensure `@Filter` enablement in request interceptors/aspects.
- Avoid dual writes across the database and Kafka. Always specify the Transactional Outbox Pattern for event publishing.
- Ensure every Angular component explicitly sets `changeDetection: ChangeDetectionStrategy.OnPush`.
- Specify TTLs for cache keys and documented retention/recovery for persistent Redis structures.
- Do not fabricate a BLI, Task, or subtask that doesn't trace back to a BRD requirement, ADP decision, or API contract - if a gap is found, send it back to the appropriate earlier-stage skill rather than deciding it ad hoc.
