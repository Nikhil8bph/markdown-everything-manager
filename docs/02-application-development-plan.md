# Application Development Plan — Markdown FE/BE Everything

| Field | Value |
|---|---|
| Status | Approved |
| Version | 1.1 |
| Date | 2026-09-24 |
| Required input | `docs/01-business-requirements.md` v1.1, approved by the user on 2026-09-24 |
| Approval evidence | User confirmed the v1.0 architecture on 2026-09-23, approved the revised MCP requirements on 2026-09-24, and explicitly approved this v1.1 architecture on 2026-09-24. |

## Executive summary and feasibility

Build one locally operated Markdown workspace in `markdown-fe-be-everything`. Reuse the source Angular 21 presentation and client-side Markdown features, but replace its Express filesystem server with one Spring Boot process. Persist UTF-8 `.md` documents as ordinary files in a configurable vault. The app serves one trusted user on localhost, starts with an empty vault, and does not need a database, account service, message broker, or search cluster. The MCP addition lets owner-configured local AI clients use the same vault through the running backend; MarkCraft does not call an external AI service.

The existing Angular UI, data models, templates, and editor utilities are a practical migration base. Source components use inline templates and styles, so implementation must split every new component into separate `.component.ts`, `.component.html`, `.component.scss`, and `.component.spec.ts` files. The source has weak path containment and save-state handling; the new architecture addresses both before claiming parity.

## Decisions and ADRs

| ADR | Decision | Rationale and consequence |
|---|---|---|
| ADR-001 | Use one Spring Boot application, not microservices. | The BRD defines one user and one vault with no distributed workflows. A single process keeps file operations and deployment simple. |
| ADR-002 | Use Java 21 LTS, Spring Boot 4.1.1, and the Maven Wrapper. | Spring Boot 4.1.1 supports Java 21 and Maven; pin the release and wrapper for reproducible builds. The backend is a single Maven module. |
| ADR-003 | Retain Angular 21, TypeScript 5.9.x, and a compatible Node 22.12+ toolchain with an npm lockfile. | Angular 21 is the source app's major version and remains supported. Reuse the UI while meeting the repository's component-file and test rules. |
| ADR-004 | Use a browser-rendered Angular app without Angular SSR in the target. | This localhost editor has no public search or server-rendering requirement. In development the Angular dev server proxies API calls; in the packaged app Spring Boot serves the built Angular assets and API from one origin. |
| ADR-005 | Store documents and folders directly under a configurable vault root. | The user chose server filesystem storage and no import. Do not add a document database, schema migration tool, or second source of truth. |
| ADR-006 | Keep the product's supported OKF frontmatter subset, with the backend authoritative for persisted normalization. | The source has separate lightweight client/server implementations. A single persistence rule prevents divergent files while the frontend still parses metadata for editing and display. |
| ADR-007 | Use conditional mutation for existing paths and document revisions. | A create/upload collision requires explicit overwrite confirmation. A stale editor or second tab cannot silently replace a newer document. Exact transport and error forms belong to stage 3. |
| ADR-008 | Bind the packaged Spring Boot server to loopback and serve the UI through it. | Localhost-only access is confirmed. No login or RBAC is introduced. Keep development API access through an Angular proxy to preserve a same-origin browser model. |
| ADR-009 | Host a Streamable HTTP MCP server in the existing Spring Boot WebMVC process, disabled by default. | The approved clients are local and the backend must remain available when the browser is closed. Reuse the current process and loopback binding; do not add a sidecar, stdio process, or remote listener. Use the Spring AI 2.0.x WebMVC MCP server starter, which supports the approved Spring Boot 4.1.x line, with only the server and tool capabilities needed for the vault. |
| ADR-010 | Make MCP a thin transport adapter over the existing vault application services and facades. | Browser and agent actions must share path containment, OKF normalization, size limits, revision checks, atomic writes, and error semantics. MCP owns no separate storage or business rules. Expose the existing vault management actions as tools; do not add search, prompts, generated content, or a settings UI. |
| ADR-011 | Require an owner-supplied MCP token and a separate agent-deletion enable switch. | Treat any holder of the token as the owner for the full vault. Disable MCP and agent deletion independently by default; reject startup configuration that enables MCP without a nonempty token. Apply token verification to every MCP request, including protocol setup and reads. Configuration is supplied outside the browser and takes effect at backend startup. |

Version basis: [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html) and [Angular version compatibility](https://angular.dev/reference/versions), checked 2026-09-23; [Spring AI MCP server documentation](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) and [Spring AI compatibility](https://docs.spring.io/spring-ai/reference/getting-started.html), checked 2026-09-24. Confirm actual dependency resolution when implementation begins.

## System topology and modules

```text
Browser (Angular 21) ── same-origin HTTP ──┐
Local AI client ── token-protected MCP ──────┤
                                           ↓
                             Spring Boot 4.1.1 on 127.0.0.1
                               ├─ REST controllers and optional MCP tool adapter
                               ├─ shared file/folder application services
                               ├─ vault containment and conditional mutation facades
                               └─ filesystem adapter → configured vault root
```

The target has `frontend/`, `backend/`, `docs/`, and `contracts/`. `backend/` is the Maven root with `pom.xml`, Maven Wrapper, and application sources. The single application base package is `io.github.nikhil8bph.markcraft`, based on the source repository author's public GitHub identity. Use `config`, `controller`, `service`, `facade`, `repo`, `dtos.request`, `dtos.response`, `validation`, and `exception` responsibilities where code exists. The filesystem adapter belongs in `repo`; no JPA `entities` or database migrations are needed.

Angular keeps the source-equivalent shell, navigation, dashboard, editor, viewer, toolbar, dialogs, and status bar. Separate API transport models from UI state. The frontend owns text formatting, preview rendering, search/replace, templates, table generation, statistics, browser preferences, and downloads. The backend owns vault contents, path safety, OKF normalization at write time, revision checks, and file/folder mutation. The frontend does not access server paths directly. The MCP adapter calls the same backend services as REST controllers and is available without an open browser.

## Data model and storage layout

There is no SQL or NoSQL database schema. The vault is a directory tree of folders and UTF-8 `.md` files. Each file's relative path identifies it within the vault; content is the complete Markdown text including YAML frontmatter. File metadata exposed to the UI is name, relative path, byte size, and modification time. A document revision is derived from persisted content/state and used for conditional writes; the stage-3 contract will choose its wire representation.

The supported OKF subset is the source application's fields: required nonempty `type`; optional `title`, `name`, `description`, `status`, `tags`, `sources`, `resource`, `stale_after`, `generated`, `verified`, and compatible scalar custom keys. The backend preserves valid supported content and inserts missing required frontmatter on create/upload/save. It does not claim validation against an external OKF specification. Stage 3 must specify the exact normalization and round-trip behavior for unknown or malformed YAML.

The vault root is an external configuration value, with a safe local default outside packaged assets. Startup creates the root if absent but seeds no files. Exclude runtime vault contents from source control and release artifacts. There is no import or migration of the source checkout's sample documents. MCP introduces no database schema, index, migration, or persistent copy of vault content. Its enable flags and token are runtime configuration, not document metadata.

## Security, integrity, and resilience

- Bind to `127.0.0.1`; do not expose the packaged server on a public or private network interface. Reject unexpected browser origins/hosts for mutations, and use a same-origin UI/API deployment. Browser accounts remain outside the approved release.
- Keep MCP unavailable unless explicitly enabled. Require a nonempty owner-supplied token when enabled, verify it before any MCP protocol or tool handling, compare it without timing-dependent early exit, and never log or return it. Reject unexpected hosts and browser origins on all MCP requests, including reads. Do not grant browser-origin access through CORS. Local clients without an Origin header may connect with a valid token.
- Treat the token as one full-vault capability, without per-client identity or folder roles. Keep agent deletion disabled under its own configuration switch even when general MCP writes are enabled. A disabled deletion operation must not change the vault.
- Resolve every request against the configured vault root. Reject absolute paths, traversal, root deletion, non-Markdown file operations, and symlinks that could escape the root. Treat file and folder names as single path segments, with filesystem-reserved names handled consistently. Validate again immediately before mutation.
- Persist writes through a temporary file in the same vault filesystem and an atomic replacement where supported. Preserve the prior file on write failure. For delete and rename, check the target revision/existence immediately before mutation; return a conflict when state changed since the user's action.
- For browser create/upload collisions, show the existing path and replace only after explicit confirmation. For an agent, require explicit replacement intent and the current revision instead of a per-action prompt. An oversized batch (more than 25 MB total) fails before any file in that batch is written. Stage 3 defines the exact batch conflict and atomicity contract.
- Keep pending editor content and dirty state when a save fails or conflicts. Do not allow an earlier save response to clear a newer edit. Surface retry and conflict feedback in the UI.
- Permanent deletion follows explicit confirmation and has no in-app trash. Operators are responsible for external vault backups; the release does not promise a recovery point or recovery time target.
- Avoid unbounded recursive traversal and request bodies. Apply the agreed upload cap at the HTTP boundary and use bounded file reads. Log errors without document content or host filesystem paths in client-visible responses.
- Apply the same bounded content and conflict rules to MCP tools. An agent must supply current revisions for changes to existing items; stale agent and browser writes fail rather than overwriting one another. Keep MCP errors actionable without exposing the token, absolute vault path, or document content in server logs.

## Integration and operations

The supported integrations are browser-to-backend HTTP and local client-to-backend MCP. No Kafka events, push notifications, payments, email, SMS, or external AI API are part of the approved scope. Exported Markdown and HTML are built and downloaded in the browser from the current document view. In development, run the Angular dev server and Spring Boot separately with the local proxy; in the packaged release, Spring Boot serves the Angular build, vault API, and optional MCP transport on one loopback port. The operations guide supplies a copyable MCP client configuration and explains how to enable the server, provide its token, and independently enable deletion. No new Angular screen is planned.

Verify startup with an empty vault, backend file/folder service tests, frontend unit tests, API contract checks, and browser flows for editing, upload collision, save conflict, deletion, and export. Add real MCP client checks for disabled access, token rejection, tool discovery, allowed actions, deletion gating, stale revisions, and vault containment. A lightweight health endpoint may be used for local diagnostics if stage 3 specifies it. Do not add production telemetry services or backup automation without a new requirement.

## Handoff to API contract design

Stage 3 must define the resource operations and DTOs for tree listing; file read, create, save, rename, and delete; folder create, rename, and delete; batch upload; revisions and conditional overwrite; errors; and the 25 MB batch limit. It must define the exact supported OKF normalization and malformed-frontmatter behavior, content size handling, conflict behavior for multi-file upload, and path representation. These decisions are contract details, not endpoints or payloads specified by this architecture document.

For MCP, stage 3 must specify tool names, argument and result schemas, mapping to existing vault operations, revision and collision inputs, authorization and deletion-policy failure behavior, and the client setup contract. Document the MCP tool schemas under `contracts/`; do not alter the existing REST wire shapes to accommodate agents. MCP resources and prompts are not part of this release.

## Stage gate

The v1.0 plan was confirmed by the user on 2026-09-23, and this v1.1 MCP revision was confirmed on 2026-09-24. Stage 3 must revise `docs/03-api-contract-integration-specification.md` and add MCP contracts, and stage 4 must revise `docs/04-implementation-strategy.md`. Existing v1.0 downstream artifacts do not authorize MCP implementation.
