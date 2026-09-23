# Implementation Strategy — Markdown FE/BE Everything

| Field | Value |
|---|---|
| Status | Approved |
| Version | 1.0 |
| Date | 2026-09-23 |
| Inputs | Approved BRD v1.0, Application Development Plan v1.0, API specification v1.0, `contracts/openapi/v1.yaml`, and `contracts/schemas/okf-frontmatter.schema.json` |
| Approval evidence | User confirmed this 14-task strategy and its one-task-per-run boundaries on 2026-09-23. |

## Strategy and delivery gates

Build the approved single-module Spring Boot filesystem API and adapt the source Angular UI inside the target's `backend/` and `frontend/`. The backend uses Java 21, Spring Boot 4.1.1, Maven Wrapper, and `io.github.nikhil8bph.markcraft`; the frontend uses Angular 21 and the approved separate component files. Keep `../../Markdown-Everything` unchanged. There is no database, messaging, authentication, or AsyncAPI work.

Every `TASK-*` below is one agent run. A run may read dependency work but changes only its own task scope and status row. Angular tasks wait for `docs/DESIGN.md` status `Ready for Angular` covering their screen scope. `ux-design-stitch` owns one UX scope per separate run; a single scope for this workspace shell, dashboard, editor, viewer, and dialogs is planned after the contract gate. Browser verification is task scoped after relevant UI tasks.

| Phase / milestone | Exit evidence | Prerequisite |
|---|---|---|
| P0 — Foundation | Reproducible backend and frontend builds; empty vault startup | Approved stage 4 |
| P1 — Vault API | Seven contracted operations pass backend and contract checks | P0 |
| P2 — Workspace UI | Approved design is implemented; source feature parity flows work against API | P1 and `docs/DESIGN.md` |
| P3 — Packaged local app | One-origin localhost build and browser acceptance evidence | P2 |

## Traceability

| BRD requirement | Architecture owner | Contract operation or schema | BLI / tasks |
|---|---|---|---|
| FR-VAULT-001 | Filesystem facade and Angular navigation | `getVaultTree`, `getDocument` | BLI-VAULT / TASK-VAULT-001; BLI-WEB / TASK-WEB-001 |
| FR-VAULT-002 | Vault service and Angular create flows | `putDocument`, `createFolder` | TASK-VAULT-002, TASK-VAULT-003, TASK-WEB-004 |
| FR-VAULT-003 | Vault service and Angular management | `moveItem`, `deleteItem` | TASK-VAULT-003, TASK-WEB-004 |
| FR-VAULT-004 | Upload service and Angular intake | `uploadDocuments` | TASK-VAULT-004, TASK-WEB-004 |
| FR-EDIT-001 | Angular editor/store and document service | `getDocument`, `putDocument`, `Revision` | TASK-VAULT-002, TASK-WEB-002 |
| FR-EDIT-002 | Angular editor and formatting controls | Client only | TASK-WEB-003 |
| FR-EDIT-003 | Angular viewer/navigation | Client only | TASK-WEB-005 |
| FR-OKF-001 | Backend normalizer and Angular metadata UI | OKF JSON Schema; `putDocument` | TASK-VAULT-002, TASK-WEB-005 |
| FR-EXPORT-001 | Angular viewer/downloads | Client only | TASK-WEB-006 |
| FR-WORKSPACE-001 | Angular dashboard/status/dialogs | Client only | TASK-WEB-001, TASK-WEB-003, TASK-WEB-006 |
| FR-WORKSPACE-002 | Angular workspace preferences | Client only | TASK-WEB-006 |
| FR-ERROR-001 | Vault revisions and Angular store | `Revision`, `ETag`, `ErrorResponse` | TASK-VAULT-002, TASK-WEB-002, TASK-WEB-004 |
| FR-BOUNDARY-001 | Vault path containment | `RelativePath`, all vault operations | TASK-VAULT-001 through TASK-VAULT-004 |
| NFR-001 through NFR-006 | Build, loopback runtime, tests | OpenAPI v1 and OKF schema | TASK-FOUND-001, TASK-FOUND-002, TASK-RUN-001, TASK-VERIFY-001 |

## Work breakdown structure

### P0 — Foundation

#### BLI-FOUND — Reproducible local application baseline

**TASK-FOUND-001 — Spring Boot skeleton.** Owner: Spring Boot. Dependencies: approved stage 4. Scope: `backend/pom.xml`, Maven Wrapper, application bootstrap/config, package skeleton, backend test bootstrap. Acceptance: Given an empty configured vault, when the backend starts, then it binds to `127.0.0.1`, creates an empty vault, and exposes no uncontracted business routes.

- **Activity: Backend foundation.** Subtasks: create the single-module Maven build at Spring Boot 4.1.1/Java 21; place the application at `io.github.nikhil8bph.markcraft`; externalize vault root and loopback binding; define shared error/response DTOs aligned to OpenAPI.
- **Activity: Verification.** Subtasks: add startup/config tests for empty vault and loopback settings; run Maven tests. Do not add database or placeholder packages.

**TASK-FOUND-002 — Angular skeleton.** Owner: Angular. Dependencies: approved stage 4. Scope: `frontend/` build configuration, source UI dependency baseline, API endpoint constants, development proxy, frontend test bootstrap. Acceptance: Given the source checkout, when `frontend` builds, then it produces browser assets without Express/SSR and `/api/v1/vault` calls proxy to the local backend in development.

- **Activity: Frontend foundation.** Subtasks: adapt Angular 21 package/build files with an npm lockfile; remove source Express/SSR entrypoints from the target; establish typed endpoint constants and test setup; retain source styles/assets needed by the approved design.
- **Activity: Verification.** Subtasks: run Angular build and a bootstrap test; verify proxy configuration targets loopback.

### P1 — Vault API

#### BLI-VAULT — Safe filesystem document service

**TASK-VAULT-001 — Path boundary, tree, and read.** Owner: Spring Boot. Dependencies: TASK-FOUND-001. Scope: vault path validator, filesystem adapter/facade, `getVaultTree`, `getDocument`. Acceptance: Given nested folders and Markdown files, when listed/read, then paths, timestamps, sizes, and revisions match OpenAPI; traversal and symlink escapes are rejected.

- **Activity: Backend engineering.** Subtasks: implement canonical vault-root containment and no-follow path checks; list only visible folders and `.md` files in contracted order; compute file/folder revisions; expose controller/service/facade/repo layers and response DTOs.
- **Activity: Verification.** Subtasks: test empty and nested trees, UTF-8 reads, non-Markdown exclusion, traversal, sibling-prefix paths, symlinks, and quoted `ETag` values.

**TASK-VAULT-002 — Conditional document writes and OKF.** Owner: Spring Boot. Dependencies: TASK-VAULT-001. Scope: `putDocument`, OKF normalization, conditional headers, atomic file replacement. Acceptance: Given a new or current document revision, when saved, then the returned bytes and revision match disk; stale or absent conditions do not overwrite content.

- **Activity: Backend engineering.** Subtasks: implement the four approved OKF normalization outcomes; enforce 25,000,000 UTF-8 bytes; require exactly one conditional header; stage writes in the vault filesystem and replace atomically where supported; map `404`, `412`, `413`, `422`, and storage failures to contract errors.
- **Activity: Verification.** Subtasks: test missing/empty/malformed frontmatter, unknown YAML preservation, create collision, stale update, write failure retention, and schema/response conformance.

**TASK-VAULT-003 — Folder creation, move, and permanent delete.** Owner: Spring Boot. Dependencies: TASK-VAULT-001 and TASK-VAULT-002. Scope: `createFolder`, `moveItem`, `deleteItem`. Acceptance: Given a current item revision, when moved or deleted, then the requested change occurs within the vault; occupied destinations and stale revisions leave source data intact.

- **Activity: Backend engineering.** Subtasks: add folder create-only service; implement file/folder move without overwrite; require source `If-Match` for move/delete; prohibit vault-root deletion and unsafe descendants; return `204` without body for delete.
- **Activity: Verification.** Subtasks: test nested folder move/delete, destination collision, stale folder revision, absent parent, root protection, and symlink escapes.

**TASK-VAULT-004 — Batch upload.** Owner: Spring Boot. Dependencies: TASK-VAULT-002 and TASK-VAULT-003. Scope: `uploadDocuments`. Acceptance: Given a valid batch at or under 25,000,000 UTF-8 bytes, when uploaded, then each result matches disk; oversized or preflight-conflicted batches write nothing.

- **Activity: Backend engineering.** Subtasks: validate every name, folder, duplicate path, size, content, and expected revision before writes; reuse the document normalizer and atomic writer; report per-file `200` or `207` outcomes as contracted.
- **Activity: Verification.** Subtasks: test create, confirmed overwrite, duplicate names, oversized batch, malformed OKF, stale revision, and an induced partial storage failure.

### P2 — Workspace UI

#### BLI-WEB — Source-equivalent Angular workspace

**TASK-WEB-001 — Shell, sidebar, and dashboard.** Owner: Angular. Dependencies: TASK-FOUND-002, TASK-VAULT-001, DESIGN ready for shell/dashboard. Scope: app shell, sidebar/tree, folder dashboard, typed tree/read service. Acceptance: Given an empty or populated vault, when the workspace opens, then the source-equivalent navigation, filtering, selection, and empty state are usable.

- **Activity: Frontend engineering.** Subtasks: adapt source shell/sidebar/tree/dashboard with standalone OnPush components and four files each; map `Node`/`Document` DTOs to view models; show loading, empty, and retryable read errors.
- **Activity: Verification.** Subtasks: colocated component/service tests and browser check for empty/nested tree navigation, keyboard focus, and responsive layout.

**TASK-WEB-002 — Editor state and saving.** Owner: Angular. Dependencies: TASK-WEB-001, TASK-VAULT-002, DESIGN ready for editor. Scope: document store, editor pane, manual/autosave, revision conflict UI. Acceptance: Given edits, when saved manually or after about one second idle, then only the latest content clears dirty state; failures retain editable text and offer retry.

- **Activity: Frontend engineering.** Subtasks: adapt source editor to four component files; track content generation and persisted revision; issue conditional create/save calls; keep pending edits through response races, errors, file switches, and mode switches; surface save/conflict feedback.
- **Activity: Verification.** Subtasks: test rapid edits, out-of-order responses, failed save, stale `412`, and reopened content; browser check typing and save shortcut.

**TASK-WEB-003 — Formatting, search, and templates.** Owner: Angular. Dependencies: TASK-WEB-002, DESIGN ready for editor/dialogs. Scope: editor actions, toolbar formatting controls, table dialog, template data and help shortcuts. Acceptance: Given an open document, when the user applies the source's formatting, find/replace, table, template, and shortcut flows, then Markdown text and cursor state update as expected.

- **Activity: Frontend engineering.** Subtasks: adapt source editor formatting/search behavior, toolbar actions, table/help dialogs, and default templates; keep every component's template/style/spec separate; ensure actions update the same dirty editor state.
- **Activity: Verification.** Subtasks: test selected-text formatting, replace one/all, table generation, template creation, and shortcut focus behavior.

**TASK-WEB-004 — File and folder mutations and upload.** Owner: Angular. Dependencies: TASK-WEB-001, TASK-WEB-002, TASK-VAULT-003, TASK-VAULT-004, DESIGN ready for navigation/dashboard/dialogs. Scope: create/rename/delete controls, file picker, drag/drop upload, collision prompts. Acceptance: Given collisions or deletion, when the user declines confirmation, then no destructive call occurs; approved operations refresh navigation and show per-file upload outcomes.

- **Activity: Frontend engineering.** Subtasks: wire contracted folder, move, delete, and batch upload calls; ask before overwriting a document or deleting an item; enforce 25,000,000-byte batch feedback; handle `207`, `409`, and `412` without losing editor content.
- **Activity: Verification.** Subtasks: test denied/accepted confirmations, batch limit, partial upload, renamed active path, and permanent deletion warnings; browser check picker and drop zone.

**TASK-WEB-005 — Preview, TOC, and OKF inspector.** Owner: Angular. Dependencies: TASK-WEB-002, DESIGN ready for viewer/editor. Scope: viewer pane, metadata parser/inspector, table of contents, interactive task list. Acceptance: Given Markdown and OKF fields, when switching to View or editing metadata, then rendering, navigation, validity state, and persisted content agree.

- **Activity: Frontend engineering.** Subtasks: adapt source viewer and OKF utility; render syntax highlighting, tables, links, and task controls; support heading scroll and metadata editing/conform action; preserve unknown supported content on round trips.
- **Activity: Verification.** Subtasks: test preview rendering, invalid `type` indicator, metadata update, TOC jump, task checkbox, and XSS-safe rendering behavior.

**TASK-WEB-006 — Export, status, and preferences.** Owner: Angular. Dependencies: TASK-WEB-003, TASK-WEB-005, DESIGN ready for toolbar/status. Scope: browser exports/print/copy, statistics/status bar, theme/font/view preferences. Acceptance: Given an open document, when exported or preferences change, then downloads contain the current content and saved preferences return after reload.

- **Activity: Frontend engineering.** Subtasks: implement source-equivalent Markdown/HTML downloads, rendered HTML copy, print, stats/cursor indicators, theme/font/view storage, word wrap, and sidebar controls.
- **Activity: Verification.** Subtasks: test download filenames/content and preference reload; browser check print/copy controls and status feedback.

### P3 — Packaged local app

#### BLI-RUN — One-origin delivery and acceptance

**TASK-RUN-001 — Packaged localhost runtime.** Owner: infrastructure. Dependencies: TASK-VAULT-004 and TASK-WEB-006. Scope: build packaging, static asset serving, runtime configuration, local run instructions under `docs/`. Acceptance: Given a clean build, when the packaged backend starts, then the Angular UI and API share one loopback origin and the vault remains outside the artifact.

- **Activity: Runtime engineering.** Subtasks: package the Angular browser build into the approved Spring Boot static-serving flow; configure SPA fallback without shadowing `/api/v1/vault`; bind loopback; document vault location and manual backup/restore procedure.
- **Activity: Verification.** Subtasks: build both projects and smoke-test assets, API, empty vault, blocked non-loopback access, and reload of a deep UI URL if present.

**TASK-VERIFY-001 — Packaged launch verification.** Owner: browser verification. Dependencies: TASK-RUN-001. Scope: packaged launch evidence under `docs/verification/`. Acceptance: Given the packaged app, when it opens from the loopback URL, then the workspace loads, its empty vault request succeeds, and the browser shows no console or network error.

- **Activity: Browser verification.** Subtasks: inspect the packaged workspace load, empty state, API network request, console, and loopback origin; capture reproducible evidence.
- **Activity: Closeout.** Subtasks: link this evidence and the individual UI task browser checks to BRD stories; keep unresolved defects open rather than marking acceptance complete.

## Task status and ownership

Only the matching row may be updated by a task run after its evidence is reviewed. The coordinating agent owns the final status transition. All implementation tasks begin `Not Started`.

| Task ID | Owner | Dependencies | Status | Updated | Evidence / blocker |
|---|---|---|---|---|---|
| TASK-FOUND-001 | spring-boot-enterprise-architect | Stage 4 approved | Completed | 2026-09-23 | Java 21 and Maven Wrapper provisioned; `./mvnw test` passed (1 startup test), `./mvnw package -DskipTests -q` passed. Empty vault, loopback setting, and absence of vault routes verified. |
| TASK-FOUND-002 | angular-enterprise-architect | Stage 4 approved | Completed | 2026-09-23 | Node 22.14, `npm ci`, production/development Angular builds, 2 bootstrap tests, and lint passed. Browser-only assets and loopback `/api/v1/vault` proxy verified; no Express/SSR dependency or output. |
| TASK-VAULT-001 | spring-boot-enterprise-architect | FOUND-001 | Completed | 2026-09-23 | `./mvnw test -q` passed 9 tests and `./mvnw package -DskipTests -q` passed. HTTP tests cover empty/nested trees, natural order, UTF-8, byte sizes, timestamps, SHA-256 revisions, quoted ETags, traversal/sibling-prefix/symlink escapes, error envelopes, and loopback Host rejection. |
| TASK-VAULT-002 | spring-boot-enterprise-architect | VAULT-001 | Completed | 2026-09-23 | `./mvnw test -q` passed 24 tests and `./mvnw package -DskipTests -q` passed. HTTP/unit checks cover conditional create/update, disk bytes and ETags, stale/collision retention, absent parents, 25,000,000-byte limits, all OKF normalization outcomes, malformed YAML, unknown fields, origin/path rejection, and induced commit-failure retention. |
| TASK-VAULT-003 | spring-boot-enterprise-architect | VAULT-001, VAULT-002 | Completed | 2026-09-23 | `./mvnw test -q` passed 32 tests and `./mvnw package -DskipTests -q` passed. HTTP tests cover folder creation, nested file/folder moves, permanent recursive deletion with `204` and no body, occupied destinations, stale file/folder revisions, missing parents/sources, required quoted `If-Match`, root/descendant protection, and symlink containment. |
| TASK-VAULT-004 | spring-boot-enterprise-architect | VAULT-002, VAULT-003 | Completed | 2026-09-23 | `./mvnw test -q` passed 44 tests and `./mvnw package -DskipTests -q` passed. HTTP/unit checks cover create, confirmed overwrite, duplicate/colliding/stale/absent paths, missing required fields, symlink containment, exact and oversized 25,000,000-byte boundaries, malformed OKF, preflight no-write guarantees, and induced partial commit with `207` per-file outcomes. |
| TASK-WEB-001 | angular-enterprise-architect | FOUND-002, VAULT-001, DESIGN | Completed | 2026-09-23 | Follow-up evidence in `docs/verification/TASK-WEB-001/README.md`: Chrome/API search matched folder `Projects` (both nested files) and filename `Beta` (one result); body-only text correctly returned none. Mobile drawer remained 390px without overflow. `npm test` passed 23 files / 74 tests; lint and production build passed under Node 22.14.0. |
| TASK-WEB-002 | angular-enterprise-architect | WEB-001, VAULT-002, DESIGN | Completed | 2026-09-23 | `npm test` passed 20 tests; production build and lint passed. Store tests cover debounce, conditional create/update transport, queued saves, newer-edit retention, failed-save retry, stale `412` review/replacement, file/mode switching, and out-of-order reads. Browser with a temporary vault verified typing, autosave and Ctrl+S persisted to disk, reload restored content, external edit produced a recoverable conflict retaining local text, and 390px editor had no horizontal overflow or console errors. |
| TASK-WEB-003 | angular-enterprise-architect | WEB-002, DESIGN | Completed | 2026-09-23 | `npm test` passed 33 tests; production build and lint passed. Tests cover formatting utilities, selection edits, literal find/replace with case matching, table bounds/alignment, dialog validation, template destinations/collisions, and shortcut/help surfaces. Browser with a temporary vault verified formatting, Ctrl+F and match counts, Replace All, table insertion, template creation via create-only PUT, collision review with current disk content, help dialog focus return, and mobile 390px layout with page scroll width 390 and no console overlay. |
| TASK-WEB-004 | angular-enterprise-architect | WEB-001, WEB-002, VAULT-003, VAULT-004, DESIGN | Completed | 2026-09-23 | Reverification in `docs/verification/TASK-WEB-004/README.md`: npm test passed 21 files / 48 tests, lint and production build passed under Node 22.14. Chrome/API checks passed create/open, reviewed conditional replacement, file and folder rename paths, exact upload payload, explicit replacement revision, upload outcomes, non-destructive backdrop close, focus return, Escape, and 640×300 dialog scrolling. |
| TASK-WEB-005 | angular-enterprise-architect | WEB-002, DESIGN | Completed | 2026-09-23 | Reverification in `docs/verification/TASK-WEB-005/README.md`: npm test passed 21 files / 65 tests, lint and production build passed under Node 22.14. Chrome/API checks passed frontmatter exclusion, unique TOC focus/scroll, task autosave, XSS filtering, external-link safety, multi-language highlighting and clipboard, all supported metadata controls and unknown-key persistence, malformed YAML rejection, dialog focus return, 640×300 internal scrolling, and 390px TOC collapse. |
| TASK-WEB-006 | angular-enterprise-architect | WEB-003, WEB-005, DESIGN | Completed | 2026-09-23 | Follow-up evidence in `docs/verification/TASK-WEB-006/README.md`: Chrome verified full-source Markdown download, sanitized HTML download/copy/print, cursor position, immediate and restored preferences with invalid-value fallbacks, hidden-sidebar recovery, persisted View mode, modal focus/Escape/return, and clipboard error feedback. `npm test` passed 23 files / 73 tests; lint and production build passed under Node 22.14.0. |
| TASK-RUN-001 | infrastructure-agent | VAULT-004, WEB-006 | Completed | 2026-09-23 | Added `scripts/package-frontend.sh`, copied Angular browser assets into Spring Boot static resources, added SPA fallback routing that leaves `/api/v1/vault/**` specific routes intact, and documented loopback launch plus vault backup/restore in `docs/operations-local-runtime.md`. `./mvnw test -q` and packaged jar build passed; jar contains `static/index.html`, JS, and CSS. |
| TASK-VERIFY-001 | task-browser-verification | RUN-001 | Completed | 2026-09-23 | Final latest-package evidence in `docs/verification/task-verify-001.md`: fresh jar served current frontend assets; Chrome verified loopback root, empty-tree `200`, no console/network errors, isolated external vault, packaged folder-name search, and 390px drawer. Frontend 23 files / 74 tests, lint/build passed; `./mvnw clean package -q` passed 44 backend tests. |

## Definition of Done

- The task's stated Given-When-Then outcome passes against the approved BRD, architecture, OpenAPI, and, for UI work, `docs/DESIGN.md` with `Ready for Angular` status for that scope.
- Java code uses Maven and the approved package/layer boundaries; Angular components have separate `.component.ts`, `.component.html`, `.component.scss`, and `.component.spec.ts` files, standalone APIs, and OnPush change detection.
- Relevant unit/integration/browser checks pass; negative paths cover conflict, invalid input, storage failure, and vault containment where applicable. No upstream artifact or source checkout is changed by a downstream task.
- The task status row records completion date and evidence. A failed or incomplete check keeps the task `In Progress` or `Blocked` with a concrete reason.

## Stage gate

This strategy was confirmed by the user on 2026-09-23. The UX handoff separately requires the approved first three stages and a `Ready for Angular` design scope before frontend tasks begin.
