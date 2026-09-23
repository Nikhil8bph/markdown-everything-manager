# TASK-WEB-002 browser verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Google Chrome 154.0.8037.57, Playwright 1.63.0, `http://127.0.0.1:8080/`, isolated vault at `/tmp/markcraft-web002`. Fixtures were created through the contracted API. The failure path used a one-time Playwright response override; all successful writes and conflict reads used the real API. No application code or planning artifacts were changed.

## Results

| Scenario | Result | Evidence |
|---|---|---|
| Manual save with Ctrl+S | Pass | Typed into the labeled editor, pressed Ctrl+S, observed Saved, and read the new content back from the API. |
| Idle autosave | Pass | Added another edit and observed it reach the API after the idle delay; the UI returned to Saved. |
| Reopened content | Pass | Reloaded the page, reopened the document, and confirmed the autosaved text was in the textarea. |
| Mode switch preserves draft | Pass | Changed the editor buffer, switched Edit → View → Edit, and confirmed the same draft text remained. |
| Save failure retains edits and retry | Pass | A controlled one-time HTTP 500 produced “Save failed — edits kept here” and Retry Save; local text remained editable, and Retry Save persisted it through the real API. |
| Stale revision (`412`) | Pass | Changed the disk copy through a conditional API write after opening the local draft. Ctrl+S produced a conflict; local edits remained, Review latest copy showed the external text in the expanded details, and the disk copy remained unchanged until an explicit choice. |
| Explicit use-disk resolution | Pass | Accepted the UI confirmation for Use disk copy; editor adopted latest server content and discarded the local draft as the user explicitly chose. |
| Out-of-order/in-flight edit race | Pass | Delayed the first successful save response, typed a newer generation while it was pending, then released the response. The queued conditional write persisted generation two and the UI returned to Saved. |
| Error-specific recovery (`404`, `413`, `422`) | Pass | Controlled API responses retained the current text and showed the corresponding missing-file/refresh, reduce-size, and fix-frontmatter guidance. The `404` state included Refresh vault and Retry Save actions. |
| Explicit overwrite-disk resolution | Pass | Reviewed a stale conflict, accepted the Overwrite disk copy confirmation, and verified the local choice replaced the external disk version through the real API. |
| Browser runtime errors | Pass | No page errors were observed in the completed checks. |

The tested flows cover the TASK-WEB-002 browser checklist, including failure, conflict decisions, error-specific recovery, reopen, and response-race behavior. The failure, `404`, `413`, and `422` responses were controlled browser fixtures; successful persistence, stale revisions, and conflict resolution used the real API. No strategy or status row was changed.
