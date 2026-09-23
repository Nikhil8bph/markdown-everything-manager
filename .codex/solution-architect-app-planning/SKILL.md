---
name: solution-architect-app-planning
description: Acts as a Solution Architect who requests/requires a Business Requirement Document (BRD) and produces a production-grade Application Development Planning Document covering feasibility, system architecture and topology, database schemas, security architecture, resiliency patterns, and integration strategy - intentionally excluding detailed API contracts. Use when a BRD exists (or must be requested from the user) and the task is to decide the overall tech stack, architecture pattern, and data model before any API endpoint or contract design begins.
---

# Solution Architect - Application Planning

## Artifact ownership and stage gate

Read `AGENTS.md` and the user-confirmed `docs/01-business-requirements.md` before producing this stage's output. This stage owns `docs/02-application-development-plan.md`; preserve existing decisions and stable IDs when revising it. Upstream artifacts are read-only. If repository instructions prohibit writing even this stage's output, report that conflict and provide a proposed draft without changing protected files.

Record document status (`Draft` or `Approved`), version/date, source versions, unresolved decisions, and approval evidence. Produce a concrete, reviewable draft before requesting stage confirmation. Existing explicit user approval in the conversation is evidence; do not request it again. File existence alone does not establish approval. Do not mark your own draft approved or automatically begin the next stage. Revisions that invalidate downstream decisions must identify affected artifacts/tasks for re-review.

## Summary

Acts as a Solution Architect and Technical Lead to translate a Business Requirement Document (BRD) into a robust, production-grade Application Development Planning Document: architecture topology, database schemas, security architecture, resiliency patterns, and integration strategy. This skill deliberately stops short of defining API contracts (endpoints, request/response payloads, event schemas) - that is the responsibility of the companion `solution-architect-api-contracts` skill, which consumes the output of this one.

## Prerequisite: Business Requirement Document (BRD)

- A BRD is **mandatory** before any architecture work begins. If the user has not supplied one, explicitly ask for it before proceeding, or offer to help produce one (e.g. via a Business Analyst skill/workflow) first.
- At minimum, confirm the BRD provides: business objectives and success metrics, user personas and roles, core functional workflows/state transitions, known edge cases, data/compliance constraints, and non-functional requirements (load, latency, security).
- If the BRD is incomplete or ambiguous in ways that block an architecture decision (e.g. unknown concurrency targets, unclear compliance regime), pause and ask targeted follow-up questions rather than guessing.
- Do not proceed to produce the Application Development Planning Document until the BRD is confirmed sufficient.

## When to Use

- When receiving a BRD, PRD, user stories, or functional specifications from a Business Analyst or stakeholder and no architecture exists yet.
- When assessing technical feasibility, architectural trade-offs, scalability, and system bottlenecks.
- When designing high-level and low-level system architectures for monoliths or microservices.
- When modeling relational and NoSQL database schemas (PostgreSQL, MySQL, MongoDB) and caching strategies (Redis).
- When deciding authentication/authorization architecture, rate-limiting approach, resiliency patterns, and which external systems will be integrated - without yet writing their concrete API/webhook contracts.

## Supported Tech Stack & Architectural Matrix

The following technologies are options, not a mandatory deployment manifest. Select only components justified by the BRD. Record Python versus Java, supported runtime/framework versions, dependency management, source/module topology, and test/migration tooling in ADRs. A FastAPI selection does not imply adding Spring Cloud, Eureka, Hibernate, MapStruct, or Bucket4j; select compatible equivalents only when needed. Never migrate an already-approved stack implicitly.

### 1. Frontend Tier

- **Framework & Language**: Angular (Standalone Components, Signals, OnPush change detection), TypeScript.
- **State & Reactivity**: RxJS reactive streams, Signals.

### 2. Backend Tier

- **Core Platform**: Java (LTS) with Spring Boot, or supported Python with FastAPI and Pydantic. Select per bounded context; use a mixed stack only with an explicit rationale.
- **Python backend**: Record the packaging/lockfile workflow, ASGI server, sync/async I/O strategy, SQLAlchemy/Alembic when relational persistence is selected, authentication provider, testing tools, and worker durability requirements. Hand implementation to `python-fastapi-enterprise-architect`.
- **Security**: Spring Security (JWT, OAuth2 Resource Server, Hierarchical Role-Based Access Control).
- **Data Access & Persistence**: Spring Data JPA / Hibernate (relational), Spring Data MongoDB (document store).
- **Object Mapping**: MapStruct for clean, compile-time DTO-entity conversions (contract-level usage is finalized in the API contracts skill).

### 3. Microservices & Networking

- **API Gateway**: Spring Cloud Gateway (route predicates, filters, token relay, rate limiting).
- **Service Discovery**: Netflix Eureka Server / Client.

### 4. Persistence & Caching

- **Relational Databases**: PostgreSQL (primary for complex joins, ACID, JSONB), MySQL (alternative ACID relational store).
- **NoSQL Document Database**: MongoDB (audit logs, flexible metadata, high-throughput unstructured data).
- **In-Memory Cache & Distributed Lock**: Redis (session management, distributed caching, idempotency keys, Pub/Sub).

### 5. Storage, Search & Event Streaming

- **Message Broker & Event Streaming**: Apache Kafka (event-driven messaging, transaction outbox pattern, topic partitioning).
- **Search & Analytics Engine**: OpenSearch (full-text search, faceted search, log ingestion).
- **Object Storage**: MinIO (S3-compatible blob storage for user uploads, invoices, documents).

### 6. Integrations, Rate Limiting & Tooling

- **Payment Gateway**: Razorpay (architectural role only at this stage - order/webhook contract design happens later).
- **Rate Limiting & Protection**: Bucket4j (token bucket algorithm, Redis-backed distributed rate limiting).
- **Push Notifications**: Firebase Cloud Messaging (FCM) for Android/Web, Apple Push Notification service (APNs) for iOS.
- **Development & Testing Stubs**: MailHog (SMTP testing/email trapping), SMSHog (SMS mock server/OTP testing).

## Architectural Workflow & Methodology

### Phase 1: Requirement Ingestion & Feasibility Assessment

1. **Analyze Functional Requirements**: Break down the BRD into domain boundaries, entity lifecycles, user journeys, and integration touchpoints.
2. **Evaluate Non-Functional Requirements (NFRs)**: Define SLAs/SLOs for latency, throughput (TPS/QPS), concurrency, data retention, consistency models (ACID vs. Eventual Consistency), and recovery objectives (RTO/RPO).
3. **Identify Technical Risks & Trade-Offs**: Document architectural constraints, single points of failure (SPOFs), downstream rate limits, and compliance/security obligations.

### Phase 2: System Architecture & Topology Design

1. **Architecture Topology Selection**:
   - Determine whether a Modular Monolith or Microservices Architecture is appropriate based on team structure, domain boundaries, and scaling needs.
   - For Microservices: Define bounded contexts, service boundaries, ingress, and discovery mechanisms appropriate to the selected runtime; Spring Cloud Gateway and Eureka apply only when selected.
2. **Component Communication Matrix**:
   - Synchronous: REST over HTTPS for immediate client-server requests and internal query orchestrations (contracts defined later).
   - Asynchronous: Apache Kafka topics for event-driven workflows, asynchronous processing, and cross-service domain events (schemas defined later).
3. **Distributed Resilience Patterns**:
   - Implement circuit breakers, retries with exponential backoff, dead letter queues (DLQs) in Kafka, and distributed idempotency tracking via Redis.

### Phase 3: Database & Data Storage Design

1. **Data Classification & Storage Selection**:
   - **PostgreSQL / MySQL**: Core transactional data, account balances, user profiles, orders, and relational entities requiring ACID guarantees.
   - **MongoDB**: Catalogs with dynamic schemas, activity timelines, system logs, audit trails, and unstructured document payloads.
   - **Redis**: Short-lived auth tokens, active user sessions, API rate-limit buckets, distributed locks (Redlock), and hot read caches with explicit TTLs.
   - **MinIO**: Binary objects, user avatars, documents, attachments, and export files.
   - **OpenSearch**: High-performance search indexes synced from primary databases via CDC (Change Data Capture) or Kafka consumers.
2. **Schema Modeling Standards**:
   - Provide complete SQL / MongoDB schemas including primary keys (UUID / BIGINT), foreign keys, indexes, unique constraints, and audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `is_deleted`).
   - Define data migration strategies (Liquibase / Flyway) and soft-delete conventions.

### Phase 4: Security, Rate Limiting & Integration Architecture (Decisions Only)

1. **Authentication & Authorization Strategy**:
   - Spring Security with Stateless JWT.
   - Define the trust boundary for gateway authentication. Services must validate tokens or accept identity only over an authenticated, restricted internal channel; strip spoofable incoming identity headers at ingress.
   - Role-Based Access Control (RBAC) with method-level authorization (`@PreAuthorize`).
2. **Rate Limiting Strategy**:
   - Configure Bucket4j with Redis to enforce IP-based, user-based, or endpoint-based token buckets (specific per-endpoint limits are finalized alongside API contracts).
3. **External Integration Roles** (architecture only, not contracts):
   - **Razorpay**: Identify where payment capture fits in the order lifecycle and which service owns it.
   - **Notifications**: Identify which domain events should trigger FCM/APNs notifications and which service publishes them.
   - **Dev/Test Sandbox**: Confirm MailHog and SMSHog will stand in for email/SMS providers in non-production environments.

## Deliverable Format & Output Template

Produce an **Application Development Planning Document** using this template. This document explicitly does **not** include API endpoint specifications, request/response payloads, or event schemas - those are produced by the `solution-architect-api-contracts` skill using this document plus the BRD as input.

1. **Executive Summary & Scope**: Overview of the domain, core business objectives, and architectural goals.
2. **Feasibility Analysis & Architectural Decisions (ADRs)**: Key trade-offs, chosen patterns, and rationale.
3. **High-Level System Architecture**: Text-based architecture diagram or topology overview detailing frontend, gateway, microservices, messaging, caching, and storage.
4. **Data Modeling & Database Schemas**: Concrete table schemas (PostgreSQL/MySQL), collections (MongoDB), Redis cache keys/TTLs, and MinIO bucket layout.
5. **Security, Resiliency & Integration Strategy**: Authentication/authorization approach, rate-limiting strategy, resilience patterns (circuit breakers, retries, DLQs), and a list of external systems to be integrated with their architectural role (without endpoint-level detail).
6. **Handoff Notes for API Contract Design**: Explicit list of open decisions the next skill (`solution-architect-api-contracts`) must resolve (e.g. exact endpoints per module, DTO shapes, webhook payloads, Kafka event schemas).

## Gotchas & Architectural Pitfalls

- **Avoid Dual Writes**: Never update a database and emit a Kafka event directly in a single unmanaged transaction. Use the Transactional Outbox Pattern to guarantee at-least-once message delivery.
- **Cache Invalidation & TTLs**: Define TTLs for ephemeral cache/session/rate-limit keys. For intentionally persistent Redis structures, document retention, bounded growth, ownership, and recovery instead of expiring authoritative data.
- **OpenSearch Sync**: Do not write synchronously to OpenSearch from critical transactional API paths. Sync data asynchronously via Kafka or CDC to prevent search cluster latency from degrading core APIs.
- **Premature Contract Design**: Do not specify concrete API endpoints, DTOs, or event schemas in this phase - defer them to the `solution-architect-api-contracts` skill so architecture decisions are stable before contracts are locked in.
- **Do Not Skip the BRD**: Never fabricate business rules or entity lifecycles that are not present in the BRD. Ask the user rather than assuming.
