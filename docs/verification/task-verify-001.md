# Packaged launch verification

Date: 2026-09-23

Commands:

```bash
java -Duser.home=/tmp/markcraft-final -jar backend/target/markcraft-0.1.0-SNAPSHOT.jar
curl http://127.0.0.1:8080/
curl http://127.0.0.1:8080/api/v1/vault/tree
curl http://127.0.0.1:8080/workspace/deep-link
```

Observed:

- Loopback origin `http://127.0.0.1:8080/` returned the packaged Angular `index.html`.
- The empty-vault API returned `{"success":true,"data":[]...}`.
- A deep UI path returned the Angular index through SPA fallback.
- Browser snapshot showed the MarkCraft shell, explorer, empty-vault state, and no browser error overlay.
- The packaged jar contained `BOOT-INF/classes/static/index.html`, JavaScript, and CSS assets.
- The process was stopped cleanly after verification.

## Repeat verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Java 21.0.12.1, loopback address `127.0.0.1`, clean temporary `user.home` at `/tmp/markcraft-task-verify-clean`, port `8081`, Google Chrome 154.0.8037.57, Playwright 1.63.0.

Automated checks rerun for this verification: frontend `npm test -- --watch=false` passed 21 files / 41 tests; `npm run lint` passed; `npm run build` passed. Backend `./mvnw test -q` passed 44 tests with no test failures or errors. Frontend dependencies were restored with `npm ci` under Node 22.14 because `node_modules` was absent at the start of this run.

### Packaged launch acceptance

| Check | Result | Evidence |
|---|---|---|
| Packaged UI opens at the loopback origin | Pass | Browser loaded `http://127.0.0.1:8081/`; title was `MarkCraft - Markdown Editor & Viewer`; MarkCraft shell rendered. HTTP returned `200`. |
| Empty vault request succeeds | Pass | Browser observed `GET /api/v1/vault/tree` returning `200`; UI showed `Vault is empty.`; response data was `[]`. |
| Browser console and network are clean on initial load | Pass | No console errors, page errors, or failed requests during the clean-vault root load. |
| Runtime listens on loopback | Pass | `ss` showed the Java listener on `[::ffff:127.0.0.1]:8081`. |
| Deep-link application route | Not applicable | The Angular router defines only the root route. The server SPA fallback returns the index for arbitrary paths, but an arbitrary path is not a supported Angular route. |

### Additional browser checks against an isolated populated vault

These exploratory checks exercise documented BRD user flows beyond the packaged-launch task. They do not change the upstream requirements, contracts, design, or implementation strategy.

| Flow | Result | Evidence |
|---|---|---|
| Create a folder and a Markdown document | Pass, partial | UI-created `UI-QA-folder` appeared in `GET /tree`; UI-created `QA-workflow.md` appeared in the tree with OKF `type: concept` frontmatter. The separate required behavior that a new document opens into the editor failed (see below). |
| Open new document after creation | Fail | Reproduction: choose New .md File, enter `QA-created-open.md`, and continue. The API created the document and the dialog closed, but the browser remained on the dashboard (`#markdown-editor` count was `0`). Route to Angular task `TASK-WEB-004`. |
| Select and read a document; manually save edits | Pass | Tree selection opened the editor. Clicking Save issued the document `PUT`; a follow-up `GET /documents?path=QA-workflow.md` returned the edited content, including preserved unknown frontmatter `custom: preserved`. |
| View mode and metadata inspector | Pass | Preview rendered document headings and showed `OKF v0.2 valid`; Metadata opened the inspector. |
| Formatting, find/replace, and table dialog | Pass | Bold formatting wrapped selected text; Find showed a match for `Details`; the table builder dialog opened. |
| Apply a template | Pass | Template choices rendered; creating `QA-template.md` produced a readable vault document via the API. |
| Export Markdown | Pass | Browser download completed with filename `QA-workflow.md`. |
| Preference value storage | Pass, partial | The selected light theme value survived reload in local storage and appeared in the Preferences dialog after reopening the document. |
| Apply theme and editor font-size preferences | Fail | Selecting Light did not change the body class or computed background color. Changing editor font size from 14px to 18px left the textarea at 13px computed font size. Route to Angular task `TASK-WEB-006`. |
| Restore view mode after reload | Fail | Reproduction: select a document, switch to View, reload, then reopen it. Edit mode was selected. Route to Angular task `TASK-WEB-006`. |
| Help and responsive layout | Pass | Help/shortcuts dialog opened. Dashboard and editor had no horizontal overflow at 390px. |
| Rename a file | Pass | UI move request returned `200`; `QA-template-renamed.md` was readable afterward. |
| Confirm deletion | Pass | Warning stated the item would be permanently deleted and could not be undone. Cancel preserved the test folder; confirming deletion returned `204` and removed it from the tree. |
| Rename a folder | Fail | Reproduction: create `UI-QA-folder`, right-click it, choose a new name `UI-QA-renamed`, and continue. The UI sent the move request, which returned `400 INVALID_PATH`; the folder remained unchanged. The UI formed the destination as a child of the folder being renamed. Route to Angular task `TASK-WEB-004` for correction. |
| Upload a Markdown file | Fail | Reproduction: open Upload .md, select `QA-upload-repro.md` containing `# Upload repro`, and submit. The observed request body was `{"folder":"","files":[{"name":"QA-upload-repro.md","content":"# Upload repro\\n","expectedRevision":null,"size":15}]}`. `POST /api/v1/vault/uploads` returned `400 INVALID_REQUEST`; no document was created. The UI-only `size` property is rejected because the backend disallows unknown JSON fields. Route to Angular task `TASK-WEB-004`; align the client payload with the approved OpenAPI schema. |
| Interactive task-list checkbox | Fail | A Markdown `- [ ] Verify task` rendered as `<li> Verify task</li>` with no checkbox input, so the documented preview toggle cannot be used. Route to Angular task `TASK-WEB-005`. |

The initial clean-vault startup met `TASK-VERIFY-001` acceptance. At that point, the additional failures showed broader BRD parity was not yet verified; subsequent task-scoped corrections are reconciled below.

## Current packaged rebuild verification

Date: 2026-09-23. Rebuilt the frontend with `scripts/package-frontend.sh` under Node 22.14, then ran `backend/./mvnw clean package -q` successfully. Frontend gates on the current source passed: 21 test files / 41 tests, lint, and production build. Invoking the frontend gates with the default Node 20.9 failed only at the Angular CLI version preflight; no tests ran under that unsupported runtime.

| Check | Result | Evidence |
|---|---|---|
| Packaged clean-vault root load in Chrome | Pass | Fresh jar at `http://127.0.0.1:8080/` displayed the MarkCraft shell and empty state. The browser observed tree `200` with `data: []`, no console/page errors, and no failed requests. |
| Loopback bind | Pass | `ss` showed only `[::ffff:127.0.0.1]:8080`; connecting to the host's non-loopback `192.168.100.52:8080` failed to connect. |
| Packaged asset set / vault separation | Pass | Jar contains `static/index.html`, main JS, styles, and both hashed chunks. The external `/tmp/markcraft-final/.markcraft/vault` exists separately; no vault documents or vault directory entries were packaged. |
| Server fallback for arbitrary path | Pass (HTTP only) | `/workspace/test` returned `200 text/html` through SPA fallback. |
| Angular handling of arbitrary path | Not applicable / runtime error if opened | Chrome booted the returned SPA document but Angular logged `NG04002: 'workspace/test'`. The app config defines only the root route, and the approved `TASK-VERIFY-001` deep-link check is marked not applicable because no non-root Angular route is specified. Root acceptance remained clean. |

The server was stopped after the checks and its isolated vault was removed. This verifies the current build/package gates and clarifies that HTTP index fallback is not equivalent to a supported Angular deep route.


## Reconciliation after task-specific corrections

The historical additional failures above were routed to their owning implementation tasks and are now corrected in their task-scoped follow-up reports: create/open, collision handling, rename, upload payload and outcomes are reverified in [TASK-WEB-004](TASK-WEB-004/README.md); task list rendering is reverified in [TASK-WEB-005](TASK-WEB-005/README.md); theme/font and view-mode restoration are reverified in [TASK-WEB-006](TASK-WEB-006/README.md). These historical observations are retained as evidence of what the initial packaged build exposed; they are no longer current open failures. A subsequent source-code cross-check clarified the dashboard search behavior: the source matches file and folder names, not body text. The folder-name mismatch was corrected and browser-verified in [TASK-WEB-001](TASK-WEB-001/README.md). Run a final packaged verification against the latest frontend build before closing the broader audit.

## Final verification with the latest packaged frontend

Date: 2026-09-23. The package was rebuilt from the current frontend with Node 22.14.0 using `./scripts/package-frontend.sh`, then assembled and tested with Java 21.0.12.1 using `backend/./mvnw clean package -q`. The Maven command completed successfully; current Surefire reports contain 11 suites, 44 tests, 0 failures, 0 errors, and 0 skipped. Frontend gates on this source passed in the preceding TASK-WEB-001 correction run: `npm test` 23 files / 74 tests, lint, and production build. The packaging build completed with existing PrismJS CommonJS optimization warnings and no style-budget warning.

Environment: fresh jar at `http://127.0.0.1:8081/`, bound to `127.0.0.1`; isolated `user.home` at `/tmp/markcraft-verify-final-home`; Chrome 154.0.8037.57 driven through a local Chrome DevTools Protocol session. The browser test began with an empty vault. It then created `Projects`, `Projects/Alpha.md`, and `Projects/Beta.md` through the live API (201 each) to verify the latest packaged folder-search correction. No API responses were mocked.

| Check | Result | Evidence |
|---|---|---|
| Packaged UI at loopback | Pass | Chrome loaded the current packaged app at `127.0.0.1:8081`; title was `MarkCraft - Markdown Editor & Viewer`, and the shell plus empty dashboard rendered. |
| Empty vault API and state | Pass | Browser observed `GET /api/v1/vault/tree` `200`; direct API response was `success: true` with `data: []`; UI showed `No folders yet`, `No .md files yet`, and `Vault is empty.` |
| Browser console and network | Pass | No page exceptions, console errors, or failed requests. Root, packaged hashed assets, favicon, and vault tree requests succeeded (assets returned 304 from cache on first reuse and 200 on reload). |
| Current packaged assets | Pass | Jar `index.html` is byte-identical to both `frontend/dist/app/browser/index.html` and `backend/src/main/resources/static/index.html`. Every asset referenced by the index exists in the jar, including `main-NWRKVZHZ.js`, `styles-ZV7SMPDP.css`, and `chunk-WZTCZCO5.js`. |
| Loopback bind and vault separation | Pass | `ss` showed Java listening on `[::ffff:127.0.0.1]:8081`. The isolated vault was under the external user home, and the jar contained no vault directory or documents. |
| Packaged folder-name search | Pass | After API fixture creation, `Projects` returned Alpha and Beta; `Beta` returned only Beta; a phrase present only in the Markdown body returned no files. |
| Packaged mobile navigation | Pass | At 390×844, document scroll width stayed 390px; Toggle explorer changed `aria-expanded` false→true and opened the sidebar drawer. |

The Java process and Chrome session were stopped after verification, and the temporary vault, Chrome profile, and fixtures were removed. The current package satisfies TASK-VERIFY-001 acceptance against the latest frontend build.
