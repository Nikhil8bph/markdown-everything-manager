---
name: solution-architect-api-contracts
description: Acts as a Solution Architect who uses an existing Business Requirement Document (BRD) together with an Application Development Planning Document to design concrete RESTful API contracts, DTOs, error formats, event/message schemas, and integration endpoint specifications for an already-decided architecture. Use when the system architecture, tech stack, and database design have already been planned and the next step is defining the actual API endpoints, request/response payloads, webhook contracts, and Kafka event schemas.
---

# Solution Architect - API Contract Design

## Framework-neutral contracts and validation

Apply framework/provider examples below only when the approved application plan selects them. Wire contracts must work independently of Java or Python implementations; MapStruct is Java-specific, while FastAPI uses typed Pydantic boundary models and explicit mapping. Neither implementation tool dictates public field names or error shapes.

Produce OpenAPI in `contracts/openapi/`, AsyncAPI in `contracts/asyncapi/` only for approved messaging, and reusable JSON Schemas in `contracts/schemas/` where needed. Use the agreed specification versions. Resolve references, validate examples against schemas, and run available specification validators; report checks that could not run. Do not create empty messaging artifacts for a REST-only application.

Specify required versus nullable versus omitted fields, formats, bounds, unknown-field policy, pagination limits, operation IDs, content types, auth/scopes, headers, error schemas, and retry/idempotency semantics. A `204` response has no body. Document validation failures explicitly rather than assuming FastAPI's default `422` or Spring's default error structure matches the contract. Review breaking changes against existing consumers before requesting approval.

## Artifact ownership and stage gate

Read `AGENTS.md` and the user-confirmed BRD and application development plan before producing this stage's output. This stage owns `docs/03-api-contract-integration-specification.md` and relevant machine-readable files under `contracts/`; preserve existing decisions and stable IDs when revising it. Upstream artifacts are read-only. If repository instructions prohibit writing even this stage's output, report that conflict and provide a proposed draft without changing protected files.

Record document status (`Draft` or `Approved`), version/date, source versions, unresolved decisions, and approval evidence. Produce a concrete, reviewable draft before requesting stage confirmation. Existing explicit user approval in the conversation is evidence; do not request it again. File existence alone does not establish approval. Do not mark your own draft approved or automatically begin the next stage. Revisions that invalidate downstream decisions must identify affected artifacts/tasks for re-review.

## Summary

Acts as a Solution Architect and Technical Lead to translate an already-approved Application Development Planning Document (system architecture, topology, database schemas, security/integration strategy) into concrete, production-grade API contracts: RESTful endpoint specifications, DTOs, standardized response envelopes, event/message schemas, and integration payloads. This skill assumes architecture decisions are locked in; it does not revisit tech stack, topology, or database design choices - it consumes them.

## Prerequisites: BRD + Application Development Planning Document

Both documents are **mandatory** before contract design begins:

- **Business Requirement Document (BRD)**: Needed to validate that every endpoint, DTO field, and event maps back to a real business rule, user story, or workflow step - not an assumption.
- **Application Development Planning Document** (produced by the `solution-architect-app-planning` skill): Needed to know the chosen architecture topology, database schemas, authentication/authorization strategy, rate-limiting approach, and which external systems (Razorpay, FCM/APNs, MailHog/SMSHog) are in scope.

If either document is missing or incomplete:

- Ask the user to provide it.
- If the Application Development Planning Document doesn't exist yet, recommend running the `solution-architect-app-planning` skill first rather than improvising architecture decisions here.
- Do not invent database schemas, topology, or security mechanisms in this skill - only reference what the planning document already decided.

## When to Use

- When the Application Development Planning Document and BRD already exist and the next step is defining concrete API endpoints and payloads.
- When defining RESTful API structures, error handling conventions, and DTO contracts with MapStruct.
- When designing Kafka event/message schemas and producer/consumer contracts for an already-decided event-driven pipeline.
- When specifying webhook and integration contracts for Razorpay, push notifications (FCM/APNs), and testing sandboxes (MailHog/SMSHog).
- When defining per-endpoint authentication, authorization, and rate-limiting rules on top of an already-chosen security architecture.

## Relevant Tech Stack for Contract Design

- **Backend Platform**: Java (LTS), Spring Boot, Spring Security (JWT, OAuth2 Resource Server) - contracts must specify which endpoints require which roles/scopes.
- **Object Mapping**: MapStruct for compile-time DTO-entity conversions - contracts define the exact RequestDTO/ResponseDTO shapes MapStruct will map.
- **API Gateway**: Spring Cloud Gateway - contracts define route paths, predicates, and any gateway-level filters (e.g. token relay) per endpoint group.
- **Event Streaming**: Apache Kafka - contracts define topic names, key/partition strategy, event payload schemas, and producer/consumer matrix.
- **Payment Gateway**: Razorpay - contracts define order-creation request/response, webhook payload shape, and signature verification headers.
- **Push Notifications**: FCM (Android/Web) and APNs (iOS) - contracts define notification payload schema and delivery triggers.
- **Dev/Test Sandbox**: MailHog and SMSHog - contracts define the mock email/SMS payloads used for verification/OTP flows in non-production environments.

## Architectural Workflow & Methodology

### Phase 1: Contract Scope Alignment

1. Cross-reference the Application Development Planning Document's module/domain boundaries with the BRD's functional workflows to produce a full list of endpoints, events, and integration touchpoints needed.
2. Confirm which endpoints are synchronous REST calls vs. asynchronous Kafka-driven flows, per the communication matrix already decided in the planning document.
3. Flag any BRD requirement that the planning document does not yet account for, and pause to clarify before writing contracts for it.

### Phase 2: RESTful API Contract Specification

1. **RESTful API Conventions**:
   - Use standard HTTP methods (GET, POST, PUT, PATCH, DELETE) and status codes (200, 201, 204, 400, 401, 403, 404, 409, 422, 500).
   - Standardize unified response wrappers:
     - Standard API Response: `{ "success": boolean, "message": string, "data": T, "timestamp": string }`
     - Paginated Response: `{ "success": boolean, "data": List<T>, "page": number, "size": number, "totalElements": number, "totalPages": number }`
     - Error Response: `{ "success": false, "error": { "code": string, "message": string, "details": [...] }, "timestamp": string }`
2. **DTO & Mapping Strategy**:
   - Maintain strict separation between database entities (already modeled in the planning document) and API DTOs (RequestDTO, ResponseDTO).
   - For approved Java implementations, use MapStruct interfaces; for Python, use explicit typed mapping; specify field-level mapping notes when entity and DTO shapes diverge.
3. **Endpoint Documentation**: For every endpoint, specify Method, Route, Required Auth/Role, Request payload, Response payload, and applicable error codes.

### Phase 3: Event & Messaging Contract Specification

1. Define Kafka topic names, key/partition strategy, and event payload schemas for every asynchronous workflow identified in the planning document.
2. Define the producer/consumer matrix: which service publishes each event, which service(s) consume it, and expected processing semantics (at-least-once, idempotent consumers).
3. Define Dead Letter Queue (DLQ) topics and the retry/backoff contract for failed event processing.

### Phase 4: Security & Rate-Limiting Contracts (Per Endpoint)

1. Apply the authentication/authorization strategy from the planning document to each endpoint: which roles/scopes are required, and how JWT claims map to `@PreAuthorize` checks.
2. Apply the rate-limiting strategy from the planning document to define concrete Bucket4j bucket keys, limits, and refill rates per endpoint or endpoint group.

### Phase 5: External Integration Contracts

1. **Razorpay**: Specify server-side order-creation request/response (`razorpay_order_id`), the webhook endpoint contract for payment capture/failure, and the HMAC SHA256 signature verification header/process.
2. **Notifications**: Specify the Kafka event schema that triggers notification delivery, and the FCM (Android/Web) / APNs (iOS) payload contract.
3. **Dev/Test Sandbox**: Specify the MailHog email contract and SMSHog OTP/SMS contract used to validate verification flows in non-production environments.

## Deliverable Format & Output Template

Produce an **API Contract & Integration Specification Document** using this template:

1. **Reference Summary**: Link back to the BRD and Application Development Planning Document this contract design is based on.
2. **API Specifications & Contracts**: Endpoint table (Method, Route, Auth/Role, Description) along with sample Request and Response JSON payloads for each.
3. **Event & Messaging Specifications**: Kafka topics, keys/partitions, event schemas, producer/consumer matrix, and DLQ configurations.
4. **Security & Rate-Limiting Contracts**: Per-endpoint auth/role requirements and Bucket4j limits.
5. **External Integration Contracts**: Razorpay order/webhook payloads, FCM/APNs notification payloads, MailHog/SMSHog test contracts.
6. **Error Handling Matrix**: Standardized error codes/messages mapped to failure scenarios across all endpoints.

## Gotchas & Contract-Design Pitfalls

- **Idempotency in Payments & Webhooks**: Razorpay webhooks can be delivered multiple times. Always record webhook payment IDs in Redis/PostgreSQL with unique constraints to prevent duplicate fulfillment.
- **MapStruct Entity Cycles**: For Java, fail on unintended unmapped target fields and explicitly ignore only reviewed exclusions. Keep JPA entity cycles out of public DTOs.
- **Contract Drift from Architecture**: Do not introduce new databases, services, or topology decisions while writing contracts - if a gap is found, send it back to the `solution-architect-app-planning` skill rather than deciding architecture ad hoc.
- **Unversioned Breaking Changes**: Always version APIs (e.g. `/api/v1/...`) and event schemas so future contract changes do not silently break existing consumers.
- **Inconsistent Pagination/Envelope Shapes**: Reuse the same standard/paginated/error response wrappers across all modules; do not let individual endpoints invent their own response shape.
