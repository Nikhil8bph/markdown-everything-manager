---
name: python-fastapi-enterprise-architect
description: Implement, scaffold, and review Python FastAPI backends from approved architecture, API contracts, and one implementation task. Covers typed API boundaries, dependency injection, async I/O, persistence, security, migrations, and testing; does not choose a new stack or rewrite upstream contracts.
---

# Python FastAPI Enterprise Architect

Implement one approved backend task using the existing Python project conventions. FastAPI is an alternative to Spring Boot when selected by the application plan, not permission to replace an existing backend.

## Required inputs and scope

Read `AGENTS.md`, then verify user approval and relevant source versions for:

- `docs/01-business-requirements.md`: business rules, acceptance criteria, NFRs.
- `docs/02-application-development-plan.md`: FastAPI selection, package/service topology, versions, persistence, security, integrations, and deployment model.
- `docs/03-api-contract-integration-specification.md` and relevant OpenAPI, AsyncAPI, and JSON Schema files under `contracts/`: exact wire behavior.
- `docs/04-implementation-strategy.md`: one backend `TASK-*`, dependencies, subtasks, acceptance criteria, Definition of Done, and status row.

If an artifact is absent, unapproved, contradictory, or does not cover the requested behavior, provide a non-mutating assessment identifying the owning earlier stage. Do not infer approval from file existence or silently switch a Java task to Python. Backend-only work does not require `docs/DESIGN.md`.

Upstream documents and everything under `contracts/` are read-only. Never regenerate authoritative contracts from `app.openapi()`. Compare a temporary generated schema against the approved schema and report discrepancies.

Before creating files, search the existing routers, schemas, dependencies, services, repositories, clients, migrations, and test fixtures. Reuse approved implementations. Follow the planned package root; do not create a second application or speculative shared framework.

## Task lifecycle

Execute exactly one `TASK-*` per run. Read sibling tasks for context only. After gates and dependencies pass, re-read and set only the selected status row to `In Progress` immediately before implementation. Preserve other workstream notes. Record owner, date, changed files, validation evidence, and blockers.

Set `Completed` only when the entire task's acceptance criteria and DoD pass, including other disciplines if the task spans them. Unavailable required checks or partial backend delivery cannot establish completion. Record `Blocked` with evidence when progress cannot continue. For an explicitly delegated run, report evidence to the coordinating parent; it alone owns the status row. Do not create delegation merely to expand task scope.

## Read the relevant implementation references

- For scaffolding, routers, DTOs, settings, dependency injection, security, or external clients: [Application and API design](references/application-and-api.md).
- For databases, transactions, async execution, migrations, jobs, or events: [Persistence and concurrency](references/persistence-and-concurrency.md).
- For tests, contract checks, deployment integration, or completion evidence: [Verification and operations](references/verification-and-operations.md).

Read only references relevant to the task. Inspect installed versions and the lockfile before using APIs; consult official documentation for compatibility-sensitive choices. Do not upgrade dependencies merely to match a sample.

## Implementation sequence

1. Map acceptance criteria to approved operations/events and existing code. Identify gaps before editing.
2. Implement the smallest cohesive slice through typed API boundaries, authorization, application logic, and persistence/integration adapters.
3. Keep transaction ownership, resource cleanup, timeout/retry behavior, and error translation explicit.
4. Run the repository's formatter/linter, type checker, and relevant unit, integration, and contract tests. Validate migrations when changed.
5. Review the diff for accidental contract changes, leaked secrets, unbounded work, duplicate implementations, and changes outside the selected task.
6. Update the selected status row based on evidence and report results, unrun checks, and remaining blockers.

Infrastructure changes belong to `infrastructure-agent` under their own approved task. Supply its required runtime/configuration information without adding unplanned deployment assets.
