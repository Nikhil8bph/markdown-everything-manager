# TASK-WEB-003 browser verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Google Chrome 154.0.8037.57, Playwright 1.63.0, `http://127.0.0.1:8080/`, isolated vault at `/tmp/markcraft-web003`. Template create/read/write/collision checks used the real API; the template offline branch used a controlled HTTP 500. No application code or planning artifacts were changed.

## Results

| Scenario | Result | Evidence |
|---|---|---|
| Selected-text formatting | Pass | Exercised all 15 supported formatting actions (H1/H2/H3, bold, italic, strike, inline code, quote, unordered/ordered/task lists, link, image, code block, horizontal rule, table). Each applied the expected Markdown to the selected range and returned focus/caret to the editor. |
| Empty-selection formatting | Pass | Repeated all 15 toolbar actions with a collapsed selection; each inserted its expected placeholder or structure. |
| Dirty state and persistence | Pass | A toolbar formatting edit showed Unsaved; Ctrl+S saved it, and a real API read returned the formatted Markdown. Ribbon was absent in View mode. |
| Find, case, navigation, replace | Pass | Ctrl+F opened the strip; case-insensitive count was 1 of 3, Match Case reduced it to two lowercase hits, Next selected `Cat`, and Replace All inserted `$&` literally at all matches. No-match showed `0 matches` and disabled Next/Replace; clearing the query cleared the count. Escape returned focus to the editor. |
| Table validation and insertion | Pass | 11 columns showed validation feedback and disabled Insert Table. Escape left content unchanged. A centered 2×2 table preview had centered separator markers; Insert placed it at the saved caret and returned focus to the editor. |
| Help and shortcuts | Pass | Help displayed nine keyboard shortcut entries and ten cheat-sheet rows. Escape returned focus to Help trigger. Browser checks for Ctrl+B/I/K/Q, Ctrl+E, Ctrl+F, Tab/Shift+Tab, and ignoring Ctrl+E while the table dialog was open matched the mapped behavior. |
| Templates tab and create | Pass | Four cards appeared. Opening Apply Template left the current document selected. A template created under an existing folder through the real API, refreshed navigation, and opened in Edit with its template content. |
| Template validation and cancel | Pass | An unsafe `../bad.md` filename showed validation and disabled creation. Escape closed the dialog and returned focus to its template card. |
| Template collision, decline, and overwrite | Pass | A create collision showed that the existing file was unchanged. Review displayed its current content; Cancel preserved it. The explicit overwrite choice then replaced it using the latest conditional revision and opened the result in Edit. |
| Stale template overwrite | Pass | Changed the disk revision after reviewing a collision. The overwrite received `412`, kept the dialog open with a stale-copy message, allowed a fresh review, and succeeded only after using the newly fetched revision. |
| Template API failure | Pass | A controlled HTTP 500 left the dialog open, retained `OfflineTemplate.md`, and displayed actionable retry guidance. |
| Narrow-screen toolbar | Pass | At 390×844, the formatting ribbon was internally horizontally scrollable (`scrollWidth` 596px) while the document page remained 390px wide with no horizontal page overflow. All controls retain their accessible names. |

Checks used real Chrome interactions and API persistence for successful writes. Controlled responses were limited to the explicit offline template error. No application source or task-status row was changed.

## Find and table follow-up

Date: 2026-09-23. Rechecked the remaining single-replacement, wrap, file-switch, and table-cancel cases in a fresh browser instance.

| Scenario | Result | Evidence |
|---|---|---|
| Previous/next wrap and selection | Pass | In `cat Cat cat`, Next selected `Cat` at `2 of 3`; Previous selected the first `cat` at `1 of 3`; another Previous wrapped to the last `cat` at `3 of 3`. |
| Replace current | Pass | Replacing the selected final `cat` with `DOG` yielded `cat Cat DOG` and updated count to `2 of 2`. |
| Search resets on file switch | Pass | Switching from the edited A draft to B closed the find strip; reopening Find showed an empty query/count, while B's `dog dog` content and A's local draft remained separate. |
| Table alignment and Cancel | Pass | One-column/one-row preview used `:---` for left and `---:` for right alignment. Cancel left editor content unchanged and restored focus to Insert table. |

These checks complete the explicit Replace current/Replace All and table cancel/alignment portions of the TASK-WEB-003 browser checklist.
