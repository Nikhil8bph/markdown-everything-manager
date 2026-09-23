# Application and API design

## Packaging and boundaries

Respect the approved Python version, package root, and dependency manager. Use the existing `pyproject.toml` and lockfile/reproducible requirements workflow. Keep runtime and development dependencies distinct; do not add uv, Poetry, or another manager alongside the selected one. Avoid import-time network calls, migrations, and mutable resource creation.

For a new approved capability, a possible layout is shown below. Adapt to the plan; create only files with a real responsibility. Python packages use lowercase snake_case, not Java reverse-DNS directories.

```text
<approved-backend-root>/
  pyproject.toml
  <approved-package>/
    main.py                  # app factory/composition
    core/                    # settings, error translation, shared policies
    dependencies.py          # dependency providers
    <capability>/
      router.py              # HTTP adapter
      schemas.py             # request/response models
      service.py             # use cases, authorization orchestration
      repository.py          # persistence queries if needed
      models.py              # persistence models if needed
      clients.py             # outbound adapters if needed
  migrations/                # if selected
  tests/                     # unit/integration/API suites
```

Keep routers thin and use `APIRouter` at approved route prefixes. Application services should be callable without HTTP objects; map domain failures to the contracted HTTP errors at the boundary. Add protocols/interfaces when an existing boundary or genuine substitution needs them, not one interface and facade for every class. Capabilities must not import another service's persistence internals.

## Typed wire models

Use the project's Pydantic generation. For a new approved Pydantic v2 project, use `model_validate`, `model_dump`, and `ConfigDict` where appropriate; do not mix v1 and v2 conventions accidentally. Model inputs and outputs separately from ORM entities and internal credentials.

Match aliases, formats, enum values, requiredness, nullability, decimal/time serialization, pagination, and unknown-field policy to the contract. In PATCH requests, distinguish omission from explicit null; `exclude_unset=True` is useful only when it implements the approved semantics. Do not silently coerce invalid data when the contract requires rejection or introduce strictness that rejects allowed input.

Declare response models, status codes, security schemes, and error responses explicitly. Serialization must exclude internal fields even when persistence models contain them. A `204` response must be empty. Map request validation, HTTP, and domain exceptions into the approved envelope; response validation failures are server defects and must not expose internal values. Never return raw exception strings or validation inputs containing secrets.

## Dependency and resource lifecycle

Use typed `Annotated[..., Depends(...)]` dependencies for trusted principals, request-scoped resources, and service composition. Use a `yield` dependency for cleanup, with rollback/close behavior on failures. Decide resource scope deliberately for streaming responses and background work; check the installed FastAPI version's dependency teardown behavior.

Use an app factory when it improves isolation. Manage application-owned clients and pools through a lifespan context, with deterministic shutdown cleanup. Avoid mixing the lifespan approach with legacy startup/shutdown handlers. Do not create event-loop-bound resources at import time. See [FastAPI lifespan](https://fastapi.tiangolo.com/advanced/events/) and [dependencies with yield](https://fastapi.tiangolo.com/tutorial/dependencies/dependencies-with-yield/).

## Settings and secrets

For approved Pydantic v2 settings, use `pydantic-settings` and typed settings models. Validate required configuration early, document environment names/defaults, and inject test settings rather than depending on a developer's `.env`. Keep secrets out of reprs, logs, traces, examples, and committed files; never log the entire settings model. See [Pydantic settings](https://docs.pydantic.dev/latest/concepts/pydantic_settings/).

## Authentication and authorization

Implement the approved identity provider and token/session transport; do not scaffold a password grant or JWT issuer simply because it appears in a tutorial. Use maintained libraries for cryptography and verification. Validate signatures, allowed algorithms, issuer, audience, expiry, and required claims according to the approved policy; never infer algorithms from an untrusted token. Account for key rotation and bounded provider timeouts.

Derive tenant and actor context from authenticated identity, then enforce role/scope, object ownership, and tenant isolation in service/query boundaries. A path/body tenant ID or forwarded identity header alone is not trusted. Test cross-user and cross-tenant access, including list queries and exports. Follow contract-specific `401`/`403` behavior and authentication headers.

Implement CORS for the approved origins. Cookie authentication needs the approved CSRF and cookie controls; CORS is not authorization. Trust forwarded proxy headers only from the configured proxy boundary. Bound uploads, pagination, filtering, and expensive work to the approved limits.

## External integrations

Reuse lifespan-managed clients with explicit connection/read/write/pool timeouts where supported. Bound concurrency and retry only classified transient failures when the operation is safe to retry. Do not retry a payment or other mutation without the approved idempotency behavior. Validate webhook signatures on the original bytes before parsing or mutating state; apply replay/deduplication policy. Do not forward client credentials to arbitrary hosts or fetch arbitrary user URLs without the approved egress/SSRF controls.
