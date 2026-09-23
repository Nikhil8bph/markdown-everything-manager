# TASK-WEB-001 browser verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Google Chrome 154.0.8037.57, Playwright 1.63.0, `http://127.0.0.1:8080/`, isolated vault at `/tmp/markcraft-web001`. No application code or planning artifacts were changed.

## Results

| Scenario | Result | Evidence |
|---|---|---|
| Empty vault dashboard | Pass | On a fresh vault the browser rendered the Markdown Vault heading and both empty folder/file messages. |
| Populated dashboard and filtering | Partial | Created `Projects/Alpha.md` and `Projects/Beta.md` through contracted APIs (201 each). Dashboard showed the folder and both recent files. Searching `Beta nested` in the dashboard returned zero results even though that phrase is in `Beta.md`; the search control appears to match filenames, not document content. The design calls this a client-side search/filter derived from the tree and does not require a content-search endpoint, so whether content matching is required is unclear. |
| Sidebar descendant filtering | Pass | Filtering for `Alpha` retained its `Projects` parent and showed `Alpha.md`; `Beta.md` was excluded. |
| Tree keyboard navigation | Pass | On a 390px viewport, focusing `Alpha.md` and pressing ArrowDown moved focus to `Beta.md`. |
| Mobile explorer drawer | Pass | At 390×844, Toggle explorer changed `aria-expanded` from false to true and applied `sidebar--open`; document scroll width stayed 390px. |
| File selection | Pass | Activating the `Alpha.md` tree item opened the document in the workspace and updated the active-file label. |
| Runtime errors | Pass | No page errors were observed during the completed checks. |

The previous strategy evidence for TASK-WEB-001 was a broader smoke test; this run provides the explicit empty-state, filter, keyboard, and responsive checks. No source or task-status row was altered.

## Folder-name search correction and follow-up verification

Date: 2026-09-23. A source-code cross-check clarified the search behavior: the source dashboard filters against the complete query in both `file.name` and `file.folder`; it does not search document contents. The original `Beta nested` query therefore did not establish a content-search defect. It did expose that the current dashboard omitted folder-name matching, which was corrected in TASK-WEB-001.

The corrected source was served by `ng serve` under Node 22.14.0 against the Spring Boot API jar under Java 21.0.12.1. Chrome 154 used a real API-backed fixture with `Projects/Alpha.md` and `Projects/Beta.md` in an isolated vault. No API responses were mocked.

| Scenario | Result | Evidence |
|---|---|---|
| Search by folder name | Pass | Searching `Projects` returned both `Alpha.md` and `Beta.md` (Search Results: 2). |
| Search by file name | Pass | Searching `Beta` returned only `Beta.md` (Search Results: 1). |
| Search body content | Pass | A phrase present only in a document body returned no results, matching the source's filename/folder-name behavior. |
| Mobile dashboard and explorer | Pass | At 390×844 the page scroll width remained 390px; the Toggle explorer control changed `aria-expanded` false→true and opened the sidebar drawer. |
| Browser errors | Pass | No runtime exceptions or console errors during the exercised scenarios. |

Final frontend gates after the correction: `npm test` passed 23 files / 74 tests; `npm run lint` passed; `npm run build` passed under Node 22.14.0. The build reports PrismJS CommonJS optimization warnings and completes successfully. The TASK-WEB-001 navigation and filtering findings are corrected and reverified.
