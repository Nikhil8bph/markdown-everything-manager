# Verification summary

Date: 2026-09-24

This index consolidates current automated and browser evidence for the approved requirements and implementation strategy. The reports record observed behavior; a passing test gate does not override a browser failure.

## Automated gates

| Gate | Result | Evidence |
|---|---|---|
| Frontend unit suite | Pass | Node 22.14: 23 files, 74 tests passed after the `TASK-WEB-001` correction run. |
| Frontend lint | Pass | `npm run lint`: all files passed. |
| Frontend production build | Pass | `npm run build` completed. PrismJS CommonJS optimization warnings are reported; the build succeeds. |
| Backend suite | Pass | `./mvnw test -q`: 11 suites, 44 tests, 0 failures, 0 errors, 0 skipped. `./mvnw clean package -q` also passed. |
| OpenAPI and OKF schema checks | Pass | OpenAPI 3.1 YAML parsed; all 92 local references resolved. Draft 2020-12 OKF schema passed meta-validation and valid/missing-type/wrong-type examples. A full OpenAPI validator is not installed, as recorded in stage 3. |
| Packaged runtime root acceptance | Pass | Clean packaged jar served the Angular root and empty vault on loopback. Chrome saw tree `200`/empty data, empty-state UI, no console errors, page errors, or failed requests. Jar contained index and hashed assets; vault remained external. |
| Packaged MCP acceptance | Pass | `./scripts/verify-packaged-mcp.sh` rebuilt and packaged the app, then an MCP Java SDK client discovered and exercised all seven tools against temporary vaults with deletion disabled and enabled. Missing/invalid token returned `401`; hostile Host/Origin returned `400`. `./mvnw test -q`: 17 suites, 56 tests, 0 failures/errors. |

Running the Angular CLI under the default Node 20.9 stopped at its version preflight; no tests ran under that unsupported runtime. All frontend gates above ran under Node 22.14.

## Browser scope

| Scope | Report | Result summary |
|---|---|---|
| Workspace navigation | [TASK-WEB-001](TASK-WEB-001/README.md) | Corrected and reverified. Chrome/API checks passed empty/populated dashboard, file and folder-name search, sidebar filtering, keyboard navigation, file selection, and mobile drawer at 390px. Body-content search remains out of source scope. Frontend suite: 74 tests passed. |
| Editing and save state | [TASK-WEB-002](TASK-WEB-002/README.md) | Manual/autosave, reopen, mode draft retention, failed-save retry, revision review/resolution, and in-flight edit race passed. |
| Formatting and templates | [TASK-WEB-003](TASK-WEB-003/README.md) | Formatting, find/replace, table, shortcut/help, template create/collision/overwrite, stale revision, and mobile toolbar flows passed. |
| Mutations and upload | [TASK-WEB-004](TASK-WEB-004/README.md) | Initial failures were reproduced and corrected. Follow-up Chrome checks passed create/open, explicit document/upload collision choices, conditional replacement, upload contract body and results, selected file rename, sibling folder rename, safe backdrop dismissal, focus return, and short-height dialog behavior. Frontend suite: 48 tests passed. |
| Preview and metadata | [TASK-WEB-005](TASK-WEB-005/README.md) | Historical browser failures have been corrected and reverified. Chrome/API checks passed frontmatter exclusion, unique heading navigation/focus, task persistence, XSS filtering, external links, Prism highlighting/copy, metadata fields and unknown-key round trip, malformed YAML blocking, dialog focus return, short-height scrolling, and mobile TOC. Frontend suite: 65 tests passed. |
| Export, status, preferences | [TASK-WEB-006](TASK-WEB-006/README.md) | Markdown download and print invocation passed. Corrected and reverified. Chrome/API checks passed full-source Markdown download, sanitized rendered HTML download/copy/print, cursor position, applied and validated theme/font/wrap/sidebar preferences, view-mode restoration, dialog Escape/focus return, and clipboard failure feedback. Frontend suite: 73 tests passed. |
| Packaged launch | [TASK-VERIFY-001](task-verify-001.md) | Rebuilt the package from the latest frontend. Chrome verified the empty root state, empty API response, current hashed assets, loopback binding, external vault, folder-name search, and mobile drawer; no console or network errors occurred. 44 backend tests passed. |
| MCP client and vault handoff | [TASK-MCP-004](TASK-MCP-004/README.md) | Packaged Java MCP client discovered all seven tools and exercised revision-aware document/upload writes, move, deletion policy, and recursive deletion. The browser showed the MCP-created marker document and saved content; console and page-error checks were empty. |

## Acceptance state

The test and build gates pass. `TASK-WEB-004` and `TASK-WEB-005` browser findings have been corrected and reverified. `TASK-WEB-006` browser findings have been corrected and reverified. All current browser findings in TASK-WEB-001, TASK-WEB-004, TASK-WEB-005, TASK-WEB-006, TASK-VERIFY-001, and TASK-MCP-004 are corrected and reverified against the latest packaged frontend. The initial failures remain as historical evidence with task-scoped correction records. The automated frontend and backend test gates pass, and no current verification findings remain. No upstream requirements, architecture, API contracts, or UX handoff were changed during these implementation and verification tasks.
