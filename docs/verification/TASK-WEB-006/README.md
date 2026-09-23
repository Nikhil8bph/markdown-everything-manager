# TASK-WEB-006 browser verification

Date: 2026-09-23

Environment: packaged Spring Boot jar, Google Chrome 154.0.8037.57, Playwright 1.63.0, `http://127.0.0.1:8080/`, isolated vault at `/tmp/markcraft-web006`. Fixture `WEB006-check.md` was created through the API (201) with valid frontmatter, a heading, two prose lines, and UTF-8 text. No application code or planning artifacts were changed.

## Results

| Scenario | Result | Evidence |
|---|---|---|
| Markdown export | Pass | Downloaded `WEB006-check.md`; bytes preserved frontmatter, heading, prose, and `café`. The approved scope describes a basename `.md`; because the fixture itself ended in `.md`, the resulting filename was indistinguishable from that expectation. |
| Rendered HTML export | Fail | Downloaded `WEB006-check.html` and it begins with a doctype and title. The content contains rendered `<h2>type: note…</h2>` and `<h1>Heading One</h1>`: raw frontmatter was included as Markdown instead of being excluded from rendered document content. |
| Copy rendered HTML | Fail | Clipboard text began with `---` and Markdown frontmatter, not HTML markup. |
| Print action | Pass (controlled browser check) | Replaced `window.print` with a counter in the page and observed one invocation after activating Print document. The native print dialog was not exercised in headless Chrome. |
| Document statistics | Partial | Status displayed `17 words · 97 characters · 11 lines · 98 UTF-8 bytes` for the fixture. No cursor line/column indicator was exposed in the status. |
| Preference persistence | Partial | Selecting light theme, font size, disabling word wrap, and hiding sidebar stored `{"theme":"light","fontSize":18,"wordWrap":false,"sidebar":false}` in localStorage. On reload the dialog restored theme/light and both checkboxes; its font-size select had an empty value. |
| Preference application | Fail | After changing preferences, body background remained `oklch(0.216 0.006 56.043)`, editor font remained `13px`, and textarea white-space remained `pre-wrap`. Preferences did not apply the selected theme, font size, or word-wrap setting to the workspace/editor. |
| View mode persistence | Fail | Switched to View, reloaded, and reopened the fixture; Edit was `aria-pressed=true`, View was `false`. |
| Export dialog dismissal | Fail | After an export choice, the dialog remained open and intercepted subsequent toolbar clicks until Close was activated. The expected workflow does not state whether export should dismiss, but this obstructs repeated actions and should be reviewed against the intended interaction. |

The browser run had no retained product changes. These observations conflict with the `TASK-WEB-006` design requirements for rendered HTML, clipboard HTML, cursor position, applied and restored preferences, and view-mode preference restoration. Route implementation corrections through the owning implementation task; this verification did not edit the task strategy or status.

## Follow-up browser verification

Date: 2026-09-23. Repeated checks on a fresh packaged instance and vault. The test exported an unsaved draft containing a script element and checked localStorage edge cases. No application code or planning artifacts were changed.

| Scenario | Result | Additional evidence |
|---|---|---|
| HTML export uses current draft | Pass for draft freshness | An unsaved edit appeared in the downloaded HTML. However, export rendered the frontmatter as content and included the raw `<script>window.__exportXss=1</script>` element, so the required sanitized viewer output is not used. |
| Malformed preferences JSON | Partial | Invalid JSON fell back to dark theme and true checkboxes, but the editor font-size control still had no selected value, including for the default 14px. |
| Invalid stored preference values | Fail | With `theme: "neon"`, `fontSize: 999`, `wordWrap: "false"`, and `sidebar: null`, both selects had empty values and the string `"false"` appeared checked. Values were not validated/fallback-normalized on load. |
| Status on edit/file switch | Partial | Metrics recalculated for a changed draft and after switching files; status showed words, characters, lines, UTF-8, and save state. Cursor line/column remains absent. |
| Export Escape behavior | Fail | Pressing Escape while the Export dialog was open left it open. The initial focus remained on the Export trigger, so the dialog's Escape handler did not receive the key. The trigger retained focus, but menu dismissal did not occur. |

The fresh run also reproduced the earlier findings: changing preferences persists values but does not change body theme, editor font, wrapping, or sidebar display. The application still resets View mode to Edit on reload.

## Keyboard and mobile follow-up

Date: 2026-09-23. Checked modal keyboard focus, clipboard rejection, and mobile layout in Chrome at 390×844.

| Scenario | Result | Additional evidence |
|---|---|---|
| Preferences modal focus trap | Fail | Opening left focus on the toolbar trigger. Pressing Tab moved focus to the Edit mode button outside the dialog. Escape did not dismiss while focus remained outside. Focusing a select manually then pressing Escape closed the dialog, but focus did not return to Preferences. |
| Export modal focus trap | Fail | Opening left focus on the Export toolbar trigger; Tab moved to Edit outside the modal. Manually focusing a dialog action then pressing Escape closed it, but focus was not restored to Export. |
| Clipboard failure feedback | Fail | With `navigator.clipboard.writeText` controlled to reject, Copy rendered the success text “HTML copied.”, showed no failure/fallback message, and did not update the polite live announcement to an error. |
| Mobile page width | Pass | At 390px viewport width the app page remained 390px wide; no horizontal page overflow was observed while opening settings/export controls. |

The dialogs are labeled modal but do not initially capture focus or trap normal Tab navigation. Their Escape behavior works only after manually moving focus into the dialog, and close actions fail to restore focus to the invoking control.

## Implementation and follow-up browser verification

Date: 2026-09-23. Verified the corrected Angular source with Node 22.14.0 and the Spring Boot API jar with Java 21.0.12.1. Browser: Google Chrome 154.0.8037.57 using a local Chrome DevTools Protocol session. The API fixture `WEB006-check.md` was created and updated through the approved API in an isolated vault at `/tmp/markcraft-web006-home`; its content included valid OKF frontmatter, a heading, prose, a raw script element, and `Café text.`. Download and print flows were exercised in the browser; no API responses were mocked.

| Scenario | Result | Evidence |
|---|---|---|
| Markdown download | Pass | Downloaded `WEB006-check.md` retained the complete source including `type: note` frontmatter and `Café text.`. |
| Rendered HTML download | Pass | Downloaded `WEB006-check.html` contained the rendered heading with its stable ID and UTF-8 text; it excluded frontmatter and the raw script element. |
| Copy rendered HTML | Pass | Clipboard success path received rendered HTML without frontmatter. A rejected clipboard write produced an actionable error in the polite live announcement and closed the dialog. |
| Print | Pass (controlled browser check) | Controlled `window.print` was invoked once. At invocation the print surface contained the sanitized rendered draft, excluded frontmatter and the raw script, and the Export dialog was closed. Native print UI is not available in headless Chrome. |
| Cursor position | Pass | Moving the editor selection to offset 8 displayed `Line 2, column 5` in the status strip. |
| Theme, font, wrap, and sidebar | Pass | Selecting Light, 18px, no wrap, and hidden sidebar immediately changed the workspace theme, editor computed font size to 18px, textarea white-space to `pre`, and desktop sidebar to `display:none`; localStorage reflected all four values. The explorer toolbar control restored the hidden sidebar and remained reachable. |
| Preference restoration and validation | Pass | Reopening Preferences restored Light/18px and the checkbox states. Invalid stored values (`neon`, `999`, string `false`, and `null`) resolved to Dark/14px/wrap enabled/sidebar enabled in the controls and workspace. |
| View mode persistence | Pass | View remained selected after a full page reload and reopening the fixture. |
| Dialog keyboard and focus | Pass | Preferences and Export captured focus inside their modal; Escape dismissed each and restored focus to its invoking toolbar control. |
| Browser errors | Pass | No runtime exceptions or browser console errors during the exercised scenarios. |

Final frontend gates: `npm test` passed 23 files / 73 tests; `npm run lint` passed; `npm run build` passed under Node 22.14.0. The build reports existing PrismJS CommonJS optimization warnings; it completes successfully without style budget warnings. The prior TASK-WEB-006 browser findings are corrected and reverified.
