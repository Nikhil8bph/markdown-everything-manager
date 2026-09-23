---
name: spring-boot-enterprise-architect
description: Design, review, and scaffold enterprise Spring Boot backends with layered architecture, JPA, security, auditing, multi-tenancy, RBAC, standardized APIs, and optional microservice infrastructure. Use for backend implementation only.
---

# Spring Boot Enterprise Architect

Use this skill for planning, reviewing, scaffolding, or implementing Spring Boot backend applications. Keep the work backend-focused and preserve the user's chosen deployment style. Do not introduce microservices, gateways, composite services, or external providers unless the approved architecture specifies them.

## Implementation-strategy workflow

Before changing backend code, read `AGENTS.md` and `docs/04-implementation-strategy.md`. Resolve the exact backend `TASK-*` task, its dependencies, acceptance criteria, subtasks, and current row in the task-status table. Use the task as the implementation scope and do not implement unrelated work.

Then read the approved BRD at `docs/01-business-requirements.md` and the relevant sections of `docs/02-application-development-plan.md`, `docs/03-api-contract-integration-specification.md`, and the referenced files under `contracts/`. The planning documents define service boundaries, persistence ownership, security, and resilience; OpenAPI, AsyncAPI, and JSON Schema files define the approved wire contracts. Do not invent endpoints, DTOs, event payloads, persistence owners, business rules, or integrations when the task and contracts do not specify them.

Verify approval evidence for all four planning stages and the relevant contracts before implementation; file existence alone is insufficient. Check the implementation-strategy document status and stage gate before implementation. If `docs/04-implementation-strategy.md` is missing, its status is not approved, the task is missing, or a dependency is incomplete, stop at a non-mutating assessment/plan. Do not scaffold backend code or change task status to `In Progress` while the stage gate is open. If the request maps to multiple tasks and the correct task cannot be determined unambiguously, ask for the task ID.

Task lifecycle is recorded in the centralized table in `docs/04-implementation-strategy.md`:

- Set the exact task row to `In Progress` immediately before the first implementation change, and record the Spring Boot skill as owner when appropriate.
- Set it to `Completed` only after all task acceptance criteria, backend subtasks, contract/persistence/security tests, and the applicable Definition of Done are satisfied. Completing only the backend portion does not complete a cross-disciplinary task.
- Use `Blocked` with a concise reason and evidence when approval, dependencies, contracts, or validation prevent progress. Never claim `Completed` when tests or required evidence are missing.
- Update only the matching row, preserve existing notes from other workstreams, and include dates/evidence without rewriting unrelated sections. Re-read the row before updating it to avoid overwriting concurrent progress.

The status update is part of the task workflow, not a substitute for implementation evidence. Keep Controller-Service-Facade-Repository boundaries, tenant/RLS behavior, migrations, messaging, security, and response envelopes aligned with the approved contracts and task acceptance criteria.

## Read-only upstream artifacts

Treat these as immutable reference inputs in every mode, including planning, implementation, review, testing, and status updates:

- `docs/01-business-requirements.md`
- `docs/02-application-development-plan.md`
- `docs/03-api-contract-integration-specification.md`
- every file and subdirectory under `contracts/`

Read them as needed, but never create, edit, delete, rename, reformat, regenerate, or otherwise write to them. Do not fix a requirement, architecture, API, event, or schema discrepancy in place. Report the exact file and issue, then block or return the decision to the appropriate earlier workflow stage. The only planning artifact this skill may update during normal task execution is the matching status row in `docs/04-implementation-strategy.md`, as described above.

## Single-task execution and delegation

- One agent run owns exactly one `TASK-*` from `docs/04-implementation-strategy.md`. Read dependencies for context, but do not implement sibling tasks in the same run or update multiple task rows.
- If the request spans multiple tasks, identify the first dependency-ready task and ask the user to split the remainder, or coordinate separate task runs. Do not silently widen scope.
- When subagent delegation is available and authorized, prefer one isolated subagent per task. Give it the exact task ID, acceptance criteria, relevant source paths, and validation expectations. Do not run agents concurrently against the same files or status row.
- The coordinating parent owns the task-status row. A delegated subagent reports changes, tests, and blockers but does not independently change that row; the parent sets `In Progress`, `Blocked`, or `Completed` after verifying the report. An agent working directly on the task follows the normal lifecycle rules above.

## Reuse, SOLID, and clean code

- Search existing services, modules, shared runtime primitives, DTO mappers, policies, repositories, migrations, generated contract models, test fixtures, and configuration before creating new code. Extend or compose an existing abstraction when it already satisfies the task; do not duplicate behavior under a new name.
- Apply SOLID concretely: keep each class focused; use strategies/policies or configuration for genuine extension points; keep interfaces substitutable and narrow; and make services depend on facades/ports rather than concrete infrastructure.
- Preserve the project's Controller/Resource -> Service -> Facade -> Repository boundaries. Do not bypass an existing layer or add a new abstraction layer without a task-traceable reason.
- Keep methods small and intention-revealing, validate at boundaries, avoid magic values/dead code/catch-all exception handling, and make transactions, authorization, idempotency, and failure behavior explicit.
- Refactor duplication only within the current task boundary and keep reusable code domain-neutral when it is placed in a shared package. Add focused tests for the reused or extended behavior.

## Multi-module build structure

Use Maven exclusively for Spring Boot builds in this project. Do not create or recommend Gradle build files, Gradle wrappers, or Gradle commands. If an approved upstream artifact requires Gradle, report the conflict and route it back to the appropriate architecture stage instead of silently introducing Gradle or converting the build.

When the approved architecture uses microservices, create one Maven root build that aggregates every backend module and is the single authority for build-wide coordinates and versions. Use a root aggregator/parent `pom.xml` with `packaging` set to `pom` and keep the Maven Wrapper at the backend root. The root build must own:

- the Java release, organization `groupId`, artifact naming, and project version convention;
- Spring Boot, Spring Cloud, and other approved BOM/dependency versions;
- plugin versions and shared compiler, test, coverage, formatting, and static-analysis configuration;
- the declared module hierarchy and repositories approved by the architecture.

Do not put service controllers, entities, repositories, or business logic in the root project. Put each independently deployable microservice in one clearly named leaf module, optionally grouped beneath a domain or `services` aggregator. Add service-internal submodules only when the approved architecture or current task requires a real boundary; do not split a small service speculatively.

```text
backend/
├── pom.xml                              # root aggregator/parent; no service code
├── shared/                              # only approved cross-service modules
│   ├── pom.xml                          # optional aggregator
│   └── <shared-module>/
│       └── pom.xml
└── services/
    ├── pom.xml                          # optional service aggregator
    ├── <bounded-context>-service/
    │   ├── pom.xml
    │   └── src/
    └── <another-context>-service/
        ├── pom.xml
        └── src/
```

Keep dependency direction explicit. Service leaf modules may consume approved shared libraries, but they must not depend on another service's implementation module or share its entities/repositories. Cross-service interaction uses the approved REST/event contract. The root build keeps versions consistent; a leaf service owns its runtime configuration, migrations, tests, and deployable artifact.

For a modular monolith, retain the same Maven root aggregator/parent and version-management principle and use domain-capability modules, but produce the deployable topology specified by the architecture rather than imitating microservice boundaries.

## Java package organization

Apply an explicit package structure to every Spring Boot topology; a monolith is not exempt. Choose one lowercase reverse-DNS product base package from the approved organization/product identity, for example `com.example.blog`. Use that product package as the application base for a single-module monolith. For microservices, give every deployable service a distinct bounded-context suffix such as `com.example.blog.identity`. Never use a generic root such as `com.example.app`, uppercase package segments, hyphens, or the same service package for unrelated deployables.

Within a single-module monolith, each microservice leaf module, or each modular-monolith capability module, separate responsibilities consistently. Use the repository's established singular/plural convention when one exists; for a new codebase, prefer this baseline and add optional packages only when code for that responsibility actually exists:

```text
com.example.blog/                      # single-module monolith base
├── config/                 # Spring and integration configuration
├── security/               # filters, principals, authorization policies
├── resources/              # HTTP resource contracts/interfaces, when used
├── controller/             # HTTP adapters/resource implementations
├── service/                # application workflows and transactions
├── facade/                 # persistence/integration orchestration boundary
├── repo/                   # Spring Data repositories and specifications
├── entities/               # JPA persistence entities
├── dtos/
│   ├── request/            # inbound API models
│   ├── response/           # outbound API models
│   └── common/             # genuinely shared API value types, when needed
├── mapper/                 # MapStruct and explicit boundary mapping
├── exception/              # domain/API exceptions and handlers
├── validation/             # custom validators
├── events/                 # approved event publishers/consumers/models
├── clients/                # approved outbound service/provider clients
└── util/                   # stateless, service-local utilities only
```

For a microservice, apply the same package split beneath its service base, for example `com.example.blog.identity.controller`, `com.example.blog.identity.service`, and `com.example.blog.identity.entities`.

For a modular monolith, keep every capability inside its own product-qualified package and repeat the applicable responsibility split inside that capability:

```text
com.example.blog/
├── config/                            # application-wide configuration only
├── security/                          # application-wide security only
├── posts/
│   ├── resources/
│   ├── controller/
│   ├── service/
│   ├── facade/
│   ├── repo/
│   ├── entities/
│   ├── dtos/request/
│   ├── dtos/response/
│   ├── mapper/
│   └── exception/
└── users/
    └── ...                            # same applicable responsibility split
```

Do not flatten a modular monolith into global `controller`, `service`, `repo`, or `entities` packages containing unrelated capabilities. Capability packages must not access another capability's internal entity, repository, or implementation packages; they communicate through approved public interfaces/events.

Do not create empty placeholder packages. Keep the Spring Boot application class at the monolith application base or microservice base so component scanning stays inside the intended application. If a single-module monolith grows several substantial bounded contexts, use coherent capability packages as shown above or the approved Maven modules; do not mix unrelated contexts in one package. Keep non-Java configuration under the matching application/module's `src/main/resources` and tests under a package-mirroring `src/test/java` tree.

## Architecture boundaries

Prefer a strict dependency direction:

```text
Resource/Controller -> Service -> Facade -> Repository -> Database
```

- Resource/controller layer owns HTTP routing, request validation, OpenAPI annotations, API versioning, and response envelopes. It must not contain business rules or repository calls.
- Service layer owns business workflows, authorization orchestration, password/session workflows, and transactions. It should not call repositories directly when the project uses a facade boundary.
- Facade layer is the persistence gateway. It coordinates repository access, mapping to entities, tenant/user context, and cross-repository operations.
- Repository layer uses Spring Data interfaces and query specifications. Keep query construction out of resources and services.
- Use MapStruct at explicit boundaries. Do not expose JPA entities directly from the API.

For projects using a `resources` package, define resource contracts and implementations there. Resource methods should accept request models and return `StandardResponse<T>` or `StandardResponse<PageResponse<T>>`. Keep request and response models separate where security or field visibility differs.

## API contracts

The following identifiers, envelopes, auth flows, roles, UUIDs, auditing, soft-delete, and session patterns are examples only where supported by the approved sources. Contract routes, payloads, status codes (including bodyless `204`), identifier types, and authentication transport take precedence. Do not invent login capabilities, token revocation rules, or role hierarchies.

Use `/api/v1` versioning and document endpoints with `@Tag`, `@Operation`, and meaningful `@ApiResponse` annotations.

The project shared kernel uses:

```java
StandardResponse<T>
PageResponse<T>
```

Preserve the envelope fields already defined by the project (`data`, `message`, `error`, `timestamp`, and `httpStatus`). Do not replace the project type with a second response wrapper without an explicit migration decision.

For paginated APIs, return `StandardResponse<PageResponse<T>>`. Include page number, page size, total records, total pages, and a list of results. Validate page size limits and reject negative page values.

Never include password hashes or internal security metadata in response models. Deliver reset/refresh tokens only through the approved authentication transport; exclude them from ordinary resource DTOs and logs. Use dedicated request/response models such as `CreateUserRequest`, `LoginRequest`, `UserResponse`, and `TokenResponse`.

For password login, accept one `identifier` plus `password`. The identifier may be a username, email address, or phone number. Normalize identifiers consistently before lookup, enforce uniqueness at the database level, and return the same authentication failure shape for all identifier types.

## Persistence and domain modeling

- Use UUID primary keys for distributed-safe identifiers.
- Put shared persistence fields in abstract `@MappedSuperclass` types.
- Use `@Version` for optimistic locking.
- Define explicit table and column names, indexes, and unique constraints for login identifiers.
- Initialize JPA collections with mutable implementations such as `HashSet`, never immutable `Set.of()` values.
- Map many-to-many relationships with explicit join tables and avoid recursive entity serialization.
- Keep validation on request models where possible; use entity validation for invariants that must hold regardless of entry point.
- Use `JpaSpecificationExecutor` for dynamic search, filtering, and sorting rather than adding an unbounded number of repository methods.

## Auditing

For audit fields:

1. Extend an abstract `@MappedSuperclass`.
2. Add `@EntityListeners(AuditingEntityListener.class)`.
3. Enable auditing with `@EnableJpaAuditing`.
4. Register an `AuditorAware<String>` or `AuditorAware<UUID>` bean.
5. Resolve the authenticated principal from `SecurityContextHolder`; use an explicit system identity for scheduled/system writes.

Keep date auditing (`createdDate`, `lastModifiedDate`) separate from application-managed soft-delete fields (`deletedDate`, `deletedBy`). Audit fields do not require custom exceptions.

## Multi-tenancy

When using Hibernate discriminator-based tenancy:

- Mark the entity tenant field with `@TenantId`.
- Resolve the current tenant from trusted request/authentication context, not an arbitrary body field.
- Store tenant context in a request-scoped or carefully cleared `ThreadLocal`.
- Clear the context in a `finally` block after every request.
- Fail closed when no tenant is present; never silently use a default tenant.
- Validate that a tenant header matches the authenticated token or principal before allowing access.
- Register Hibernate's `CurrentTenantIdentifierResolver` and test repository access across two tenants.

`@TenantId` sets the entity value and participates in tenant filtering; it is not a complete authorization policy by itself.

## Soft delete and active state

Soft delete must be explicit and consistent. A delete operation should set deletion metadata and inactive state, while restore should reverse it according to authorization rules.

Do not assume `isDeleted` and `isActive` automatically filter repository results. Implement repository specifications, Hibernate filters, or service-level query policies. Define different visibility rules for ordinary users, managers, and administrators, and test each role.

## Authentication and sessions

- Store only BCrypt/Argon2 password hashes; never log or return raw passwords.
- Issue short-lived access tokens and longer-lived refresh tokens.
- Rotate refresh tokens on every refresh and invalidate the previous token.
- Store refresh/session state and token revocation data in Redis when Redis is part of the architecture.
- On logout, invalidate the refresh token and blacklist the access-token JTI until its expiry.
- If concurrent sessions are limited, store device/session metadata and enforce the configured eviction or rejection policy atomically.
- Password reset tokens must be cryptographically random, short-lived, single-use, and invalidated after reset. Invalidate existing sessions after a password reset.
- Add OAuth2/social login only when requested; link external identities by provider subject, not email alone.

## RBAC and authorization

Model role precedence explicitly rather than relying only on role names. A typical hierarchy is:

```text
ADMIN > SUBADMIN > MODERATOR > MANAGER > CUSTOMER_SUPPORT > USER
```

Enforce authorization at multiple relevant boundaries:

- Method-level security for endpoint permissions.
- Service/facade checks for ownership and role precedence.
- Specifications for filtering query results.

Do not trust a path variable such as `/users/{id}` to prove ownership. Verify the authenticated principal, tenant, target owner, and role permissions before reading or mutating data.

## Modular monoliths and microservices

Use the topology selected in the approved application plan. If topology is not established, return that decision to the architecture stage. Use separate Maven modules for shared kernel and domain capabilities when the approved architecture calls for them, apply the package structure in **Java package organization** inside every capability, and avoid leaking domain internals through the shared module.

Introduce microservice infrastructure only when justified:

- Eureka or another registry for service discovery.
- Spring Cloud Gateway for routing, centralized JWT verification, CORS, and rate limiting.
- Composite/BFF services for deliberate read aggregation or write orchestration; consult the user before adding one.
- Kafka for asynchronous events and sagas when synchronous transactions cannot cross service boundaries safely.

When microservices are approved, apply the root aggregator, leaf-service ownership, dependency isolation, and package rules in **Multi-module build structure** and **Java package organization**. Do not create separate unmanaged repositories/build roots with drifting framework or plugin versions unless the approved architecture explicitly requires a multi-repository model and defines centralized version governance.

## Configuration and operations

- Keep global configuration in `application.yaml` and environment-specific values in profile files.
- Never commit database, Redis, OAuth, payment, or messaging credentials. Use environment variables or a secret manager.
- Keep dev/test integrations such as MailHog, SMSHog, mock payment webhooks, and local brokers behind profiles.
- Use `ddl-auto: validate` outside disposable development databases and prefer migrations for shared environments.
- Add rate limiting for authentication, password reset, OTP, and other abuse-sensitive endpoints.
- Verify payment webhook signatures before changing payment state.

## Implementation workflow

1. Read the approved topology and resolve one implementation task.
2. Identify bounded contexts and module dependencies.
3. Map the approved roles, precedence, ownership, and tenant rules.
4. Implement the approved request/response contracts and error handling.
5. Model entities, constraints, audit fields, tenant fields, and repositories.
6. Implement facades, then services, then resource/controller implementations.
7. Add authentication, sessions, filters/specifications, and rate limiting.
8. Add focused unit, repository, security, tenant-isolation, and API tests.
9. Verify configuration, generated mappings, migration behavior, and startup before expanding scope.

## Review checklist

- Maven is the only backend build system; the root contains `pom.xml` and the Maven Wrapper, with no Gradle build files, wrappers, or commands.
- A microservice backend has one root aggregator/parent that owns consistent dependency and plugin versions.
- Each deployable service is an isolated leaf module; the root contains no service business code, and services do not depend on one another's implementation modules.
- Every monolith and microservice has a valid reverse-DNS base package and consistently separated `config`, `security`, HTTP, service, facade, repository, entity, DTO, mapping, and error responsibilities as applicable.
- A modular monolith keeps each bounded context in a capability package/module with its own applicable layer split; unrelated capabilities are not flattened into global layer packages or coupled through internal entities/repositories.
- Java tests mirror production packages, and configuration/migrations are owned by the matching monolith application or service/capability module.
- No resource/controller directly calls a repository.
- No service bypasses the facade boundary where the architecture requires facades.
- All API responses use the shared generic response envelope.
- Request and response models do not leak secrets.
- Login supports the intended identifier types consistently.
- Tenant context is trusted, required, cleared, and tested.
- Audit dates and principals are populated for authenticated and system writes.
- Soft-deleted records are excluded or included deliberately by role.
- Unique constraints and normalization prevent duplicate login identifiers.
- Secrets are externalized and profile configuration is complete.
