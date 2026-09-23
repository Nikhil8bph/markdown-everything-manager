---
name: ux-design-stitch
description: Create and maintain the UX design handoff for this product from approved requirements, architecture, and API contracts using the Google Stitch MCP. Produces the canonical `docs/DESIGN.md` consumed by the Angular implementation skill; do not use for application code or contract changes.
---

# Ux Design Stitch

Create a contract-aware UX design handoff before frontend implementation. The design must explain the user experience and screen behavior without changing business requirements, architecture, API contracts, event schemas, or implementation code.

## Preconditions and source order

Before designing, read `AGENTS.md` and verify that these source artifacts exist and are approved:

- `docs/01-business-requirements.md` — personas, scope, user stories, acceptance criteria, NFRs, accessibility, and configurable limits.
- `docs/02-application-development-plan.md` — Angular/web architecture, service boundaries, security, responsive/accessibility decisions, and integration constraints.
- `docs/03-api-contract-integration-specification.md` — approved routes, DTOs, envelopes, errors, pagination, ETags, idempotency, authorization, and rate-limit behavior.
- Every relevant file under `contracts/openapi/`, `contracts/asyncapi/`, and `contracts/schemas/` — exact field names, enums, states, event-driven UI implications, and contract references.

Read the relevant implementation task in `docs/04-implementation-strategy.md` when it exists so screens and flows can be traced to frontend tasks, but do not invent or modify implementation tasks. The UX skill consumes the three upstream artifacts; it does not replace the implementation strategy stage.

If any required source is missing, unapproved, contradictory, or not traceable to the requested design scope, stop at a non-mutating design assessment and report the exact blocker. Do not resolve upstream gaps in the design or by changing the source documents.

## Read-only source boundaries

Treat `docs/01-business-requirements.md`, `docs/02-application-development-plan.md`, `docs/03-api-contract-integration-specification.md`, and every file under `contracts/` as immutable reference inputs. Never create, edit, delete, rename, reformat, regenerate, or otherwise write to them. If the requested UX conflicts with them, report the conflict and return it to the appropriate product, architecture, or API-contract stage.

The only generated project artifact owned by this skill is `docs/DESIGN.md`. It may also save explicitly requested Stitch references or previews in a clearly scoped design-artifact directory, but never place generated design material inside `contracts/`.

## One design scope per run

Handle exactly one UX design scope per run, such as authentication, workspace navigation, page editing, databases, collaboration, notifications, or import/export. A scope may contain its complete user flow and screen states, but do not design unrelated product areas in the same run.

If the request covers multiple scopes, choose the first dependency-ready scope only when the dependency order is unambiguous; otherwise ask the user to split the request. When subagent delegation is available and authorized, prefer one isolated subagent for research, flow analysis, or accessibility review per design scope. The coordinating parent owns the Stitch session and `docs/DESIGN.md`; delegated agents report findings and must not concurrently edit the same design artifact or Stitch project.

## Build the Stitch brief

Before calling Stitch, derive a concise design brief from the approved sources:

1. Identify the target persona(s), goal, entry points, permissions, and success criteria from the BRD.
2. Identify the relevant Angular shell, navigation, state, accessibility, responsive, and authentication constraints from the application plan.
3. Map every user action that touches the backend to the approved OpenAPI operation, response envelope, error code, pagination/ETag/idempotency behavior, or event family. The brief may describe loading, empty, error, retry, conflict, permission-revoked, offline/reconnect, and success states, but it must not invent a new wire contract.
4. Define screen inventory, flow transitions, component responsibilities, keyboard behavior, focus order, announcements, validation, and responsive breakpoints.
5. Search existing design decisions and reusable Angular components before introducing new visual patterns. Prefer one coherent design system and reusable states over screen-specific decoration.

## Google Stitch MCP usage

Use the Google Stitch MCP when it is available and authorized. Discover the available Stitch tools and their input schemas before making a call; do not guess tool names, project IDs, screen IDs, or argument shapes. Use the MCP to create or update the requested design scope, capture the resulting screen/project references, and review the output against the brief.

If the Stitch MCP is unavailable, disconnected, or does not support the required operation, stop and report that dependency. Do not fabricate Stitch output, screenshots, URLs, IDs, or a completed `DESIGN.md`. A text-only assessment may still record the missing MCP capability and the prepared design brief, but it must not claim the design is ready for Angular.

Do not use Stitch output as permission to invent API behavior, persistence, security rules, or frontend architecture. Treat generated visual/code suggestions as design references; Angular implementation remains governed by the approved contracts, the application plan, the implementation strategy, and `docs/DESIGN.md`.

## `docs/DESIGN.md` handoff

Write or update `docs/DESIGN.md` only after reviewing the Stitch result. Preserve all unrelated scopes and their review evidence. A ready scope does not make draft scopes ready; Angular must check the requested scope’s status and source versions. Keep it concise, traceable, and implementation-ready. It must contain:

- document status: `Draft`, `Ready for Angular`, or `Superseded`; maintain the same status and source-version fields for each design scope;
- date, design scope, source document IDs/versions, and Stitch project/screen references when available;
- product UX principles and the information architecture relevant to the scope;
- screen inventory with purpose, entry/exit, persona/permission, route or shell location, responsive behavior, and Stitch reference;
- user-flow transitions and interaction rules, including loading, empty, error, retry, validation, conflict, disabled, permission-revoked, offline/reconnect, and success states where applicable;
- reusable component patterns and visual tokens rather than page-specific one-offs;
- accessibility requirements: semantic structure, accessible names, keyboard path, focus management, live-region announcements, non-color status, reduced motion, contrast, zoom/reflow, and screen-reader behavior;
- mapping from user actions and visible data to the relevant OpenAPI/AsyncAPI/schema references, without copying or changing contract definitions;
- Angular handoff notes that identify what is design intent versus what must be resolved from the approved contract at implementation time;
- review evidence, unresolved decisions, and the conditions for `Ready for Angular`.

Set `Ready for Angular` only when the scoped flow has reviewed Stitch references, complete primary states, accessibility behavior, responsive behavior, source traceability, and no unresolved contract or architecture conflict. If any of those are incomplete, keep the document `Draft` or `Blocked` in the notes and do not hand it to Angular as final.

## Angular handoff contract

The Angular skill must read `docs/DESIGN.md` before implementing any UI task and treat a `Ready for Angular` design as the UX source of truth for the scoped flow. Angular may implement the design but must not silently rewrite `docs/DESIGN.md`, upstream documents, or contracts. If implementation reveals a UX conflict, the issue returns to this skill; if it reveals a business, architecture, or wire-contract conflict, return it to the appropriate earlier stage.

## Quality and handoff checklist

- Exactly one design scope was handled.
- BRD, application plan, API specification, and relevant contracts were read and remain unchanged.
- Stitch project/screen references are captured and reviewable.
- Every primary flow has complete states and a keyboard/accessibility path.
- Reusable components and tokens are identified before screen-specific patterns.
- Visible data and actions map to approved contracts without invented fields or operations.
- `docs/DESIGN.md` status accurately reflects review completeness.
- The handoff identifies source versions, unresolved decisions, validation evidence, and the next Angular task(s).
