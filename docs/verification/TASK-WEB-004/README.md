# TASK-WEB-004 browser verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Java 21.0.12.1, Google Chrome 154.0.8037.57, Playwright 1.63.0, `http://127.0.0.1:8080/`, isolated vault under `/tmp/markcraft-web004`. The approved `TASK-WEB-004` design scope is `Ready for Angular`; this run changed no application code and used no mocked API responses.

## Results

| Scenario | Result | Evidence |
|---|---|---|
| Create folder | Pass | UI create returned successfully; the new path appeared in `GET /api/v1/vault/tree`. |
| Create Markdown file | Partial | Create-only `PUT` returned `201` and stored a normalized document. The new file did not open in the editor; after creation `#markdown-editor` was absent and the dashboard remained visible. |
| Document collision confirmation | Fail | Reproduction: create `TASK004-collision.md`, then try to create the same name. The second create returned `412`; existing content remained unchanged, but the dialog showed “The item changed on disk.” and offered no overwrite confirmation or accept/decline choice. The contract requires an explicit overwrite prompt. |
| Rename a file | Partial | Move request returned `200` and the new path was readable. When renaming the selected active file, the editor closed and the selected path was cleared after tree refresh. |
| Rename a folder | Fail | Reproduction: create `UI-QA-folder`, right-click it, enter `UI-QA-renamed`, and continue. The UI submitted a destination under the source folder; API returned `400 INVALID_PATH`, leaving the folder unchanged. |
| Delete confirmation | Pass | The warning named the target and stated deletion could not be undone. Cancel preserved the folder; a separately confirmed test-folder deletion returned `204` and removed it from the tree. |
| Choose upload file | Fail | Selecting `TASK004-upload.md` and submitting sent `POST /api/v1/vault/uploads` with an extra `size` field in each file object. The API returned `400 INVALID_REQUEST`; no file was written. |
| Drop a Markdown file | Partial | A synthetic `DataTransfer` dragover/drop event with a `File` queued `TASK004-drop.md` in the dialog (23 bytes). Submit was not successful because it uses the same invalid upload payload described above; an OS-originated drag gesture was not exercised. |
| Reject an oversized batch | Pass | Selecting a 25,000,001-byte file displayed the 25 MB limit warning, disabled Upload, and sent no additional upload request. |
| Per-file upload outcomes / HTTP 207 | Partial | The real picker request still fails validation before a real result can render (extra `size` field). A controlled `207` browser response rendered `created`, `replaced`, `failed`, and `notAttempted` rows correctly and generated one request without replay. This verifies rendering only; live UI-to-API 207 integration remains blocked by the request-shape defect. |
| Unsupported upload file | Fail | Selecting `unsupported.txt` through the picker silently left the list empty and displayed no per-file rejection reason; the design requires a per-file reason for unsupported files. |
| Upload name collision choice | Fail | Selecting an existing filename queued it without any collision or overwrite choice. The one real upload request still contained `size`, failed with `400`, and left the existing document unchanged. The required explicit replacement flow was unavailable. |
| Delete Escape behavior | Pass | From the item actions, opened the permanent-delete warning, pressed Escape, observed no DELETE request, and confirmed the document remained readable. |
| Delete backdrop dismissal | Fail | Clicking the mutation dialog's backdrop left the dialog open (the component has no backdrop-dismiss handler). No DELETE request occurred and the file remained intact. The design lists backdrop dismissal as a non-destructive exit. |

Observed API sequence included create `201`, collision `412`, file move `200`, and upload `400`. Browser console errors were the expected failed-resource messages for the `412` and `400` responses; there were no page-level JavaScript exceptions.

The browser findings contradict the current `TASK-WEB-004` Completed evidence row in `docs/04-implementation-strategy.md`. Per the repository workflow, the stage-4 planning artifact is read-only during verification; the coordinating owner must route these failures to the Angular implementation task and update status after reviewing the fix.

## Correction and follow-up browser verification

Date: 2026-09-23. Rechecked the corrected Angular source using `ng serve` (Node 22.14.0) against the packaged Spring Boot API (Java 21.0.12.1) with an isolated `user.home` vault. Browser: Google Chrome 154.0.8037.57. The browser session used only temporary `WEB004-*` fixtures.

| Scenario | Result | Evidence |
|---|---|---|
| Create and open a Markdown file | Pass | `WEB004-open.md` was created with the contracted create-only `PUT`; the editor opened it and displayed the active filename. |
| Document collision choice | Pass | Recreating that path showed a review step before replacement. Review fetched the current document, and confirmation sent a conditional `PUT` with its quoted `If-Match` revision; the replaced file opened. |
| Rename file and keep active draft open | Pass | Renaming selected `web004-renamed.md` produced `POST /moves` with `from: web004-renamed.md` and `to: web004-active-renamed.md`; the editor remained open at the new path. |
| Rename folder destination | Pass | Renaming `WEB004-rename-dir` sent `from: WEB004-rename-dir`, `to: WEB004-renamed-dir`; the resulting folder appeared at the vault root. |
| Upload request shape and result | Pass | Picker upload of `web004-upload.md` returned `created`. Captured JSON contained only `name`, `content`, and `expectedRevision` per file; no UI-only `size` property was sent. |
| Upload collision choice | Pass | An existing file required an explicit replacement choice. The accepted replacement sent its current unquoted revision in `expectedRevision`, returned the per-file outcome, and did not replay the request. |
| Upload preflight | Pass | Unit checks cover a 25,000,001-byte UTF-8 payload, exact UTF-8 byte counting, unsupported extension feedback, replacement approval and decline. |
| Delete prompt dismissal | Pass | Opened the permanent delete confirmation then dismissed through the backdrop. The dialog closed and no `DELETE` request was observed. |
| Dialog focus, Escape, and short viewport | Pass | Upload opened with focus in its folder selector; Escape closed it and restored focus to the Upload button. Mutation dialog focused the filename input, Escape closed it and restored focus to New .md File. At 640×300, the mutation dialog stayed within the viewport and remained scrollable. |
| Browser errors | Pass | No page JavaScript errors or console errors were observed in the exercised flows. |

The original findings above are retained as historical evidence. Corrections addressed their reproduced causes: create success now opens the returned document; `412 PATH_EXISTS` provides review and conditional replacement; rename computes a sibling destination and rekeys open file/folder-descendant drafts; upload strips presentation-only data, preflights UTF-8 bytes, reports unsupported files, and gates collisions on explicit choice; dialogs support Escape, focus return, backdrop dismissal, and short-screen scrolling.

Final frontend gates after the corrections: `npm test` passed 21 files / 48 tests, `npm run lint` passed, and `npm run build` passed under Node 22.14.0. These results and the browser evidence satisfy the `TASK-WEB-004` acceptance scope.
