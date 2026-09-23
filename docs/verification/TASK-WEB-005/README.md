# TASK-WEB-005 browser verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Java 21.0.12.1, Google Chrome 154.0.8037.57, Playwright 1.63.0, `http://127.0.0.1:8080/`, isolated vault under `/tmp/markcraft-web005`. The approved `TASK-WEB-005` design scope is `Ready for Angular`. Browser checks used real API reads/writes and direct filesystem fixtures for invalid documents; no API responses were mocked. No application code was changed.

## Results

| Scenario | Result | Evidence |
|---|---|---|
| Render headings, tables, and fenced code | Partial | H1/H2 content, a Markdown table, and fenced code rendered. The code element retained `language-js`; syntax highlighting was absent (no highlighted spans). |
| XSS-safe rendering | Pass | The rendered DOM contained no `script` elements or event-handler attributes. A `javascript:` link had no `href`; `window.__xss` remained unset. |
| External link behavior | Fail | A normal `https://example.com` Markdown link rendered without `target` or `rel` attributes. The design requires safe external-link target/rel behavior. |
| Code copy action | Fail | The rendered fenced code block had no copy button. |
| Table of contents | Fail | TOC entries were present, but rendered heading elements had empty IDs. Activating “Navigation Target” left the `.rendered` scroll position at `0`; the rendered heading could not be found by the generated `heading-N` target, and focus remained on the TOC button. Duplicate headings likewise had no distinct IDs. |
| Interactive task list | Fail | `- [ ] Check this task` rendered as a list item without any checkbox input. |
| Valid metadata edit and save | Partial | The valid inspector opened; changing the title updated the same draft, and the conditional document save returned `200`. The follow-up API read showed the title persisted. |
| Preserve unknown YAML fields on metadata update | Fail | Initial frontmatter included `custom: keep-this`; using Conform to change the title removed that key from the editor draft and stored document. |
| Missing `type` validity and recovery | Pass, partial | A filesystem fixture with a title and unknown key but no `type` showed “OKF metadata needs attention”; the inspector said “Required type is missing” and disabled Conform. Cancel left raw text editable. Conform with `type: note` inserted the type into the draft. However, that conform operation also removed the unknown key, as above. |
| Malformed YAML validity | Fail | A filesystem fixture with `type: note` and an unclosed `tags: [not-closed` value was displayed as `OKF v0.2 valid`; the inspector showed “Valid metadata” and enabled Conform. The frontmatter parser checks the `type` line without validating YAML syntax. |
| Supported metadata field coverage | Fail | The inspector exposed only Type and Title controls. The approved schema includes additional supported fields; those fields could not be edited through this UI. |

### Reproduction fixtures

`Viewer-rich.md` was created through the contracted create-only document endpoint with valid OKF frontmatter, an unknown `custom` key, 24 paragraphs, duplicate headings, a table, fenced JavaScript, a task marker, unsafe and safe links, an image with an event handler, and a script element. `invalid-type.md` and `malformed-yaml.md` were direct files in the isolated vault to exercise read-only invalid-content states that the backend normalizer will not create.

Observed API writes were `201` for the initial fixture and `200` for the conditional metadata save. The saved response demonstrated the unknown-field loss. There were no page JavaScript errors or failed network requests in this viewer run.

These results contradict the current `TASK-WEB-005` Completed evidence row in `docs/04-implementation-strategy.md`. That stage-4 artifact is read-only during verification; the coordinating owner must route failures to the Angular implementation task and update status after reviewing corrections.

## Follow-up browser verification

Date: 2026-09-23. Repeated the key acceptance paths against a fresh isolated packaged instance and fresh `Viewer-check.md`, `Invalid-type.md`, and `Malformed-yaml.md` fixtures. No application code or planning artifact was changed.

| Scenario | Result | Additional evidence |
|---|---|---|
| TOC navigation | Fail, reproduced | Four headings had empty `id` values. Activating the `Navigation target` TOC button left `.rendered.scrollTop` at 0 and focus on the TOC button; the matching heading was not focused. |
| Frontmatter rendering | Fail | The valid frontmatter block appeared as an extra rendered H2 and as a TOC entry, ahead of the Markdown body. |
| Task list interaction | Fail, reproduced | Both task markers rendered with zero checkbox inputs; there was no task control to toggle or persist. |
| XSS sanitization | Pass, reproduced | No script/event-handler elements remained, the unsafe link had a null `href`, and `window.__xss` remained unset. The safe HTTPS link still had no `target` attribute. |
| Metadata round trip | Fail, reproduced | Conform updated the title and explicit save persisted it, but removed `custom: keep-this`. The inspector exposed only Type and Title despite version/status/author/tags being present and supported. |
| Missing type | Pass for validity / partial recovery | Viewer showed “OKF metadata needs attention”; inspector disabled Conform and left the raw document unchanged when canceled. |
| Malformed YAML | Fail, reproduced | An unclosed `tags` sequence with `type: note` displayed “OKF v0.2 valid” and left Conform enabled. |

The repeated observations confirm that the existing `TASK-WEB-005` findings are not isolated to the earlier fixture or browser session.

## Responsive and focus follow-up

Date: 2026-09-23. Tested a long-heading viewer at 390×844 and the metadata inspector at 640×300 (a narrow viewport representative of 200% zoom on a 1280px-wide display).

| Scenario | Result | Evidence |
|---|---|---|
| Mobile TOC collapse | Pass | At 390px width, Table of contents toggled closed and removed the entries; the page remained 390px wide with no horizontal overflow. |
| Draft through preview mode | Pass | Entered a draft in Edit, switched to View and back, and confirmed the exact draft text remained. |
| Metadata dialog keyboard focus | Fail | Opening Metadata left focus on the invoking button outside the dialog. Escape did not close the dialog. Cancel closed it but focus was not restored to the Metadata button. |
| Metadata dialog at short height | Fail | At a 640×300 viewport the dialog bottom was at y≈350, below the viewport; computed overflow was `visible` with no internal scroll. The page itself had no scroll area to reach the clipped bottom. Horizontal page overflow remained absent. |

This confirms the viewer's mobile TOC control works, while metadata dialog focus return/Escape and short-screen scrolling do not meet the approved accessibility and responsive behavior.

## Correction and follow-up browser verification

Date: 2026-09-23. Verified the corrected Angular source with `ng serve` under Node 22.14.0 and the Spring Boot API jar under Java 21.0.12.1. Browser: Google Chrome 154.0.8037.57. The app used an isolated vault under `/tmp/markcraft-web005-home`; `Viewer-rich.md` was created through the contracted API and malformed YAML was a direct filesystem fixture. No API responses were mocked.

| Scenario | Result | Evidence |
|---|---|---|
| Frontmatter and Markdown preview | Pass | The viewer rendered the body without a frontmatter heading or TOC item. Tables and the `javascript` code fence rendered. |
| Heading IDs and TOC navigation | Pass | Heading IDs were `navigation-target`, `duplicate-heading`, and `duplicate-heading-2`. Activating the second duplicate TOC entry focused that heading and scrolled `.rendered` to 139px. |
| Interactive task update | Pass | The enabled checkbox updated `- [ ] Check this task` to `- [x] Check this task`; an API read after autosave confirmed persistence. Unit coverage also verifies ordered-list and multiple-task index mapping. |
| XSS and external links | Pass | Script and event-handler markup were absent and did not set `window.__viewerXss`; the `javascript:` link had no `href`. HTTPS links had `target="_blank"`, `rel="noopener noreferrer"`, and a new-tab accessible label. |
| Fenced code and copy | Pass | JavaScript, Python, Java, and YAML grammars produced Prism tokens. Copy Code was keyboard accessible; Chrome clipboard readback matched `const answer = 42;` and the live region announced success. |
| Schema and source metadata controls | Pass | The inspector exposed all JSON Schema fields (`type`, `title`, `name`, `description`, `status`, `tags`, `sources`, `resource`, `stale_after`, `generated.by/model`, `verified.by/at`) plus the preexisting `version` and `author` fields. |
| Metadata save and unknown-field round trip | Pass | Editing title and author saved through the existing document API. A follow-up read confirmed the new values, `custom: keep-this`, nested `custom_generated`, and Markdown body remained intact. YAML comments and unknown nested keys are also covered by unit tests. |
| Missing type and malformed YAML | Pass | Missing-type metadata remained correctable. An unclosed YAML sequence displayed the invalid state, showed the parse error, and disabled Apply / Conform; the raw Markdown remains available in Edit mode. |
| Metadata keyboard focus | Pass | Opening focused the Type field. Escape and Cancel both closed the dialog and restored focus to Edit OKF metadata. |
| Short viewport and mobile TOC | Pass | At 640×300, dialog height was 268px and the metadata fields scrolled internally (66px client / 1,491px content). At 390px, page scroll width remained 390px and collapsing the TOC hid its entries. |
| Browser errors | Pass | No page exceptions or console errors during the exercised flows. |

The corrections address the historical failures above. YAML is parsed with the direct `yaml` dependency; malformed syntax, duplicate keys, and schema field-shape errors are surfaced. Updates modify the YAML document while retaining unknown keys, nested extras, comments, and body text. Angular's normal HTML sanitizer remains active; safe heading IDs, task inputs, and copy buttons are attached after sanitized HTML is bound.

Final frontend gates: `npm test` passed 21 files / 65 tests; `npm run lint` passed; `npm run build` passed under Node 22.14.0. Angular reports non-ESM optimization warnings for PrismJS and its grammar modules; the build completes successfully. The browser findings and Definition of Done for `TASK-WEB-005` are satisfied.
