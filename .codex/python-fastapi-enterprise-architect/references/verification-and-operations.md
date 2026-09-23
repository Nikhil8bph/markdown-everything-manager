# Verification and operations

## Meaningful test coverage

Use repository-selected pytest tooling. Test service rules independently, API boundary behavior through the ASGI application, and persistence with the actual selected database engine when dialect, constraints, locks, or transaction behavior matters. SQLite is not proof of PostgreSQL semantics.

For synchronous tests, use `TestClient` as a context manager when lifespan is required. For async tests, use the selected async pytest plugin and HTTPX `AsyncClient` with `ASGITransport`. HTTPX's transport does not start lifespan automatically: use the project's explicit lifespan fixture/manager. Choose a compatible event-loop backend for the driver. Clear dependency overrides and close clients/sessions even when a test fails. See [FastAPI async tests](https://fastapi.tiangolo.com/advanced/async-tests/).

Select cases from the actual task:

- Success and invalid/missing/null input; exact status, body, headers, pagination, and error envelope.
- Unauthenticated, wrong scope, wrong owner, and cross-tenant access.
- Rollback, uniqueness/concurrency conflicts, and no partial side effects on failure.
- Duplicate requests/events, transient downstream failures, and timeouts.
- Startup/shutdown cleanup, migration upgrade, and cancellation for affected resources.

Mocks may isolate adapters; include integration evidence where mocks cannot establish the required behavior. Use disposable test data. Never access production credentials or perform real payment/notification effects through ordinary tests.

## Contract verification

Generate `app.openapi()` only into a temporary artifact. Compare operation IDs, method/path pairs, requiredness/nullability, aliases, formats, response codes, media types, headers, and security against approved OpenAPI. Account for generated input/output model differences semantically rather than relying on a byte-for-byte diff. Schema similarity alone does not prove actual runtime responses comply: validate representative responses too.

Use the existing spec/schema validators. Do not add an API operation, rewrite contracts, or loosen validation just to make a comparison pass. Route unexplained differences to the contract stage. Validate event payloads against approved schemas when messaging is in scope.

## Tooling and runtime handoff

Run the repository's commands for formatting/linting, type checking, and tests (for example Ruff, mypy or Pyright, and pytest only if selected). Report exact commands/results and missing tools; do not claim unrun checks passed. Do not install a second toolchain merely for this skill.

Provide the infrastructure task owner with the approved ASGI import/factory entrypoint, settings, port, health behavior, resource ownership, graceful-shutdown needs, and migration command. Development reload is not a production setting. Worker count multiplies client/database pools and process memory; do not choose a worker count without the deployment resource budget. Do not start a durable scheduler in every web worker.

Use the approved liveness/readiness endpoints and semantics; adding unspecified endpoints requires contract reconciliation. Liveness should not amplify a dependency outage into restart loops. Structured logs must exclude request secrets and sensitive payloads; propagate approved correlation IDs and use bounded-cardinality metrics. Report configuration names, never credential values.

## Completion evidence

Report the task ID, changed files, source/contract traceability, checks and results, migration evidence where applicable, known limitations, and the task's resulting status. A running development server or passing happy-path test alone does not establish completion. Keep project reports under `docs/` when a persistent report is needed.
