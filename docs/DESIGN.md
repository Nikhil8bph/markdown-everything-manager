# MarkCraft UX Design Handoff

| Field | Value |
|---|---|
| Document status | Ready for Angular for **Workspace navigation**, **Page editing**, **Editor tools**, **File mutations/upload**, **Preview/OKF inspector**, and **Export/status/preferences** scopes |
| Date | 2026-09-23 |
| Design scope | Workspace navigation, page editing, editor tools, file mutations/upload, preview/OKF inspector, and export/status/preferences; each scope has its own status below |
| Source versions | Approved BRD v1.0; Application Development Plan v1.0; API specification v1.0; OpenAPI v1.0; OKF schema as approved in stage 3 |
| Stitch project | `projects/9423112390778364876` |
| Stitch design system | `assets/14841597212643379249` |
| Next Angular task | `TASK-WEB-006` |

## Design intent and information architecture

Preserve the source `Markdown-Everything` workspace: a compact top toolbar, fixed explorer at the left on wide screens, dashboard in the main region when no document is open, and a slim status bar below. The workspace has **Edit** and **View** modes only. Stitch generated variants are composition references; the approved source app determines labels, controls, and visual density. The generated sample files in the populated screen are illustrative and must not be seeded into the empty vault.

The owner enters at the dashboard. The explorer shows folders and Markdown files, plus Explorer, Outline, and Templates tabs. For this scope, Explorer is implemented; Outline and Templates keep their source locations but their detailed behavior belongs to later UI tasks. Selecting a file exits the dashboard into the approved editor/viewer flow. The navigation scope includes the placement of New Folder, New .md File, Upload .md, and Refresh controls; their mutation dialogs and collision flows belong to `TASK-WEB-004`.

## Screen inventory

| State / location | Purpose and entry | Exit and responsive behavior | Stitch reference |
|---|---|---|---|
| Empty vault dashboard, workspace root | First load when `getVaultTree` returns `[]`; shows zero files, create/upload affordances, and no seeded documents. | Create/upload controls launch later flows. Under 768px, explorer becomes a dismissible drawer; dashboard fills width and action buttons wrap. | `projects/9423112390778364876/screens/93bb2066294943628056af67bbbcbd60` |
| Populated vault dashboard, workspace root | Shown when no document is open and tree has folders/files; includes folder summary, recent file list, search/filter, and active tree selection. | Selecting a file opens it; at 768–1023px reduce sidebar width and stack dashboard cards; at 1024px+ use source-like sidebar/main split. | `projects/9423112390778364876/screens/c41d255fd43142e4a96855ecaf6119f5` |

No account, cloud, AI, settings page, third mode, or additional route is part of this design. The toolbar may show Edit and View affordances according to the source, but this scope does not design editor or viewer content.

## Navigation states and transitions

| State | Behavior and feedback | Contract |
|---|---|---|
| Loading | Keep shell visible; show a labeled loading indicator in the explorer/dashboard region. Disable file selection until tree data resolves. | `getVaultTree` |
| Empty | Show `Markdown Vault`, zero file/folder counts, upload drop area, New Folder, and New .md File. Do not render fictitious files. | `getVaultTree` returns `data: []` |
| Populated | Render folders before files, preserve nested hierarchy, show file size and updated time when present, and keep current selection visible. Sidebar filtering retains parent folders for matching descendants. | `getVaultTree` `Node[]` |
| Read error | Show a non-color error message in the affected region with Retry; keep the shell and any currently open document state intact. | `ErrorResponse` from `getVaultTree` or `getDocument` |
| Retry | Retry only the failed read and announce successful recovery. | `getVaultTree` / `getDocument` |
| File selected | Indicate the active tree item with text/shape as well as emerald color; request the document, then transition to Edit or View according to the saved preference. A failed read leaves the selection recoverable. | `getDocument`, `ETag`, `Document` |
| Refresh | Preserve the current filter and expanded folder state while refreshing the tree; if a selected path disappeared, report it and return to the dashboard. | `getVaultTree` |
| Offline or backend unavailable | Show a connection error and Retry in place; no new storage action is attempted. | Network failure, no new wire contract |

For this scope, validation and conflict states for create, upload, rename, and delete are deferred to the separate mutation design scope. Permission-revoked state does not apply because the approved app has no accounts or roles.

## Reusable patterns and visual tokens

- Source-like stone-900 main surface, stone-950 recessed areas, stone-800 dividers, light text, and emerald-500 accent. Use Inter for interface text and monospace only for paths, sizes, and timestamps. Keep the source's compact control density and restrained rounded corners.
- Use one tree-row pattern with icon, filename, optional metadata, active marker, hover state, and visible focus ring. Use one dashboard action-button pattern and one inline status/error pattern across empty, loading, and populated states.
- The sidebar remains 256px on wide screens. At 768–1023px it may narrow while preserving readable labels; below 768px it is a drawer opened from the toolbar. At 200% zoom, avoid horizontal page scroll and allow action rows to wrap.
- Generated Stitch copy such as localhost badges, sample file names, synthetic storage totals, and “OKF Standard” labels is illustrative. Show only data returned by the API and the source app's supported OKF v0.2 wording. Do not reproduce generated empty boxes or decorative controls that have no source behavior.

## Accessibility and keyboard behavior

- Use semantic header, navigation, main, and status regions. Give every icon-only control an accessible name. Expose the folder tree with expandable state, active item, and sensible arrow-key/Enter behavior; provide a visible focus indicator separate from hover.
- Restore focus to the explorer trigger when a narrow-screen drawer closes. After a file read error, focus the error heading or Retry control without discarding the previous selection. Announce loading completion, empty state, and errors through a polite live region; use an assertive announcement only for a blocking failure.
- Show active/error/success state with text or icon as well as color. Maintain readable contrast in all three supported themes, honor reduced-motion preferences, and keep navigation usable at 200% zoom and with keyboard alone.

## Action-to-contract handoff

| User action or visible data | Approved contract | Angular responsibility |
|---|---|---|
| Initial tree, refresh, folder counts, recent files, filter input | `getVaultTree`; `Node[]` with `path`, `type`, `size`, `updatedAt`, `revision`, `children` | Derive counts and client-side filter from the tree; no new search endpoint. |
| Open file from tree or dashboard | `getDocument`; `Document` and `ETag` | Request by encoded relative path, retain revision for later conditional save, and show read failures. |
| Create, upload, rename, delete entry points | `putDocument`, `createFolder`, `uploadDocuments`, `moveItem`, `deleteItem` | Keep controls in source locations; detailed confirmation, validation, and error UI belongs to the mutation design scope. |

## Review evidence and readiness

Stitch produced and saved the two screen references above. Both desktop screenshots were reviewed against the source Angular shell/dashboard. A first generation invented a Split mode and unrelated controls; the screens were revised, and this handoff explicitly excludes those elements. The populated screen's sample data is not a seed requirement. Loading, empty, populated, read-error, retry, offline, responsive, and keyboard states are specified here without changing approved contracts.

**Ready for Angular:** `TASK-WEB-001` workspace navigation. This section does not cover editor, viewer, formatting, mutation dialogs, upload, export, or preferences. No unresolved contract or architecture conflict remains for `TASK-WEB-001`.

## Page editing scope — `TASK-WEB-002`

| Field | Value |
|---|---|
| Scope status | Ready for Angular |
| Date | 2026-09-23 |
| Sources | Approved BRD v1.0 (`FR-EDIT-001`, `FR-ERROR-001`, US-002); Application Development Plan v1.0; API specification v1.0; OpenAPI v1.0 |
| Stitch project and design system | `projects/9423112390778364876`; `assets/14841597212643379249` |
| Reviewed screens | Loaded editor: `projects/9423112390778364876/screens/822ea91f0be14da4a99c409462dfb56d`; saving with queued edits: `projects/9423112390778364876/screens/57b08d94883d4ac882c8fbb75cbd765a`; save failure/conflict: `projects/9423112390778364876/screens/b5184781f2e04dadacd79189581cd704` |
| Angular task | `TASK-WEB-002` only |

### Intent and screen inventory

The single trusted workspace owner selects an existing `.md` file from the explorer or dashboard. The same workspace route replaces the dashboard with a raw Markdown editor. Keep the current 54px toolbar, explorer, and bottom status strip; do not introduce a new route. The active filename/path, **Edit** mode, explicit **Save** action, and textual save state remain visible. The editor uses a labeled, keyboard-accessible textarea on a stone-950 canvas; content is monospace and may show a restrained line count. The document returned by `getDocument` is the initial text and revision. The illustrative files, localhost badges, line counts, sync badges, and extra actions in Stitch images are not product data or requirements.

| State | Entry, visible behavior, and exit |
|---|---|
| Loading document | Selection starts `getDocument`; keep shell and selection visible, show a labeled busy state in the main pane, and prevent editing until the read completes. |
| Loaded/saved | Show exactly the returned Markdown text. Textual “Saved” state is valid only for the buffer generation acknowledged by the server. Focus the editor on an explicit open action without stealing focus after background refresh. |
| Dirty | Each user edit updates the local buffer immediately and shows “Unsaved”. Start or reset an approximately one-second idle timer. The latest text remains visible through mode or file switches and request races. |
| Saving | Manual Save or Ctrl+S sends immediately; idle save sends after the timer. Show “Saving…” while a request is in flight. If the user types again, keep the newer generation “Unsaved” even if the older request succeeds; queue or serialize the next conditional write against the newly returned revision. |
| Save succeeded | Apply the returned normalized persisted text and revision only if doing so will not replace newer local edits. Acknowledge “Saved” only when the current buffer is the acknowledged generation; otherwise continue with “Unsaved” and save the newer generation. |
| Failed save/offline | Preserve the editable buffer and show “Save failed — edits kept here” with **Retry Save**. Retry only when the request can safely use the current revision. Do not clear the notice just because another edit occurred. |
| Revision conflict (`412`) | Preserve local text and show “This file changed on disk” in a persistent inline notice. Offer **Review latest copy** (a fresh read) and an explicit decision before replacing either copy. Never silently reload the editor buffer or blindly retry with a stale `If-Match`. A confirmed replacement of the disk copy must use its newly read revision; a decision to use the disk copy must warn that local edits will be discarded. |
| Missing/invalid/oversized | `404` prompts tree refresh while retaining local edits; `422 INVALID_FRONTMATTER` asks the owner to fix malformed YAML before retry; `413` asks to reduce size below the approved limit. All retain the buffer and provide a relevant action. |

The Edit/View selector remains in its source toolbar location. `TASK-WEB-002` preserves local content when switching modes; the detailed rendered View pane belongs to `TASK-WEB-005` and must use its own ready design scope before implementation. Formatting ribbon, find/replace, table generation, metadata inspector, templates, and export are likewise outside this page-editing design pass.

### Reusable behavior, accessibility, and responsive rules

Use the workspace’s existing stone-900/950 surfaces, stone-800 dividers, emerald active accent, compact source-like controls, and Inter interface type. Keep one save-status pattern in the toolbar and status strip, plus one persistent inline notice pattern for errors and conflicts. Status must use words and an icon/shape in addition to color. Save controls should remain reachable while a failure notice is present. Avoid a blocking modal for background save failure.

The editor has an explicit accessible name such as “Raw Markdown editor for [filename]”. Tab moves from toolbar to editor to status actions; Ctrl+S invokes Save and prevents browser Save As. Focus the failure/conflict heading or Retry action when a save fails, without losing the editable buffer. Announce state changes through a polite live region; use an assertive announcement only for a blocking conflict. Return focus to the editor after resolving a notice. At widths below 768px, the explorer is the existing drawer and editor fills the viewport; at 768–1023px the explorer narrows. At 200% zoom there is no horizontal page scroll, toolbar actions wrap or collapse with accessible names, and the textarea remains usable. Respect reduced motion and maintain visible keyboard focus and contrast.

### Contract and implementation handoff

| Visible action/data | Approved contract | UI responsibility |
|---|---|---|
| Open selected file; read retry; review server copy | `getDocument`, `Document`, quoted `ETag`, `404` | Read by vault-relative path. Retain unquoted `revision` or quoted ETag for the next conditional save. A refresh or failed read must not erase dirty text. |
| Manual save, Ctrl+S, autosave | `putDocument` with `If-Match: "<current revision>"` and complete Markdown `content`; normalized `Document` response | Serialize/coordinate in-flight writes and buffer generations so older success cannot mark newer edits saved. The backend normalizer may change content when save succeeds. |
| Conflict/retry/fix feedback | `ErrorResponse`; `412 REVISION_CONFLICT`, `404 NOT_FOUND`, `413 PAYLOAD_TOO_LARGE`, `422 INVALID_FRONTMATTER`, `500 STORAGE_FAILURE` | Keep local text and present an action specific to the returned code. A conflict requires a new read and explicit owner choice before any replacement write. Network failure has the same buffer-retention rule. |

The source app’s one-second debounce is design intent; exact HTTP payloads and quoted revision syntax come from OpenAPI. The save-state generation algorithm belongs to Angular implementation, while the user-visible invariant is that new unsaved text is never falsely labeled Saved. No authentication, cloud state, event stream, or new API route is introduced.

### Review and readiness

The loaded editor, saving-with-queued-edits, and failure/conflict Stitch screens were retrieved by screen ID and reviewed as composition references. Stitch introduced illustrative filenames, localhost labels, and some extra badges/actions; they are explicitly excluded above. The approved BRD, architecture, API specification, and contracts remain unchanged. Loading, dirty, saving, saved, offline/failure, conflict, retry, keyboard, and responsive states have defined behavior. No unresolved business, architecture, or wire-contract decision remains for `TASK-WEB-002`; **Page editing is Ready for Angular**. Other UI scopes remain outside this readiness claim.

## Editor tools scope — `TASK-WEB-003`

| Field | Value |
|---|---|
| Scope status | Ready for Angular |
| Date | 2026-09-23 |
| Sources | Approved BRD v1.0 (`FR-EDIT-002`, `FR-WORKSPACE-001`, US-006); Application Development Plan v1.0; API specification v1.0; OpenAPI v1.0 |
| Stitch project and design system | `projects/9423112390778364876`; `assets/14841597212643379249` |
| Reviewed screens | Formatting/find: `projects/9423112390778364876/screens/aebefc5e6d074ba1aa0637bc6449058d`; table dialog: `projects/9423112390778364876/screens/5ebab005508c40d3bcd448e75d2558d4`; Templates sidebar: `projects/9423112390778364876/screens/94f186cbae1d4b089ae4a6ca24a1fae9`; apply-template dialog: `projects/9423112390778364876/screens/0e8fb4d874ac41ef9953337b527af6e4`; shortcut help: `projects/9423112390778364876/screens/64b55bf9034a4941b92f38647db87d79` |
| Angular task | `TASK-WEB-003` only |

### Intent, location, and screen inventory

The single trusted owner uses these aids while editing an open document in the existing workspace route. Formatting and find/replace manipulate the current local Markdown buffer and feed the `TASK-WEB-002` dirty/autosave state; they do not call a search or formatting API. The formatting ribbon appears only in **Edit** mode with a document open. The source's Tools/Help affordances remain compact in the toolbar. At narrow widths, group or scroll toolbar actions without hiding their accessible names. Do not add a third mode or a new page.

| Surface | Entry, purpose, and exit | Stitch reference |
|---|---|---|
| Edit toolbar and find strip | With an open document, formatting actions apply Markdown syntax at the current textarea selection. Ctrl+F or the Find control opens a slim inline find/replace strip above the editor; Close or Esc restores editor focus. | `screens/aebefc5e6d074ba1aa0637bc6449058d` |
| Insert Markdown Table | Table toolbar action opens a centered modal over the editor. Columns (1–10, default 3), rows (1–20, default 3), Left/Center/Right alignment, and live Markdown preview lead to Insert Table or Cancel. | `screens/5ebab005508c40d3bcd448e75d2558d4` |
| Templates sidebar | Existing Explorer / Outline / Templates tab row switches to client-side source templates. A card starts a new document from that template without changing the current file. | `screens/94f186cbae1d4b089ae4a6ca24a1fae9` |
| Apply Template | A scoped dialog shows selected template title, an existing destination folder (including Root), editable `.md` filename, and Create from Template/Cancel. A create collision requires an explicit owner choice. | `screens/0e8fb4d874ac41ef9953337b527af6e4` |
| Shortcut help | Help control opens the source's Markdown Syntax & Keyboard Shortcuts dialog with Keyboard Shortcuts and Markdown Cheat Sheet tabs. Close/Esc returns to the trigger. | `screens/64b55bf9034a4941b92f38647db87d79` |

The source supplies four template types: Knowledge Concept, Technical Guide, API Reference, and Task & Roadmap. They are starting content, not files seeded into a new vault. Stitch's sample filenames, localhost badges, “Settings” control, fictitious counts, generated metadata, and other decorative actions do not belong to the product. The source's template sample text must not be treated as an authoritative description of the new API.

### Interaction and state rules

- Preserve textarea selection and cursor when a toolbar action receives focus, then replace only that range with the source action's Markdown syntax. Supported actions are H1/H2/H3, bold, italic, strikethrough, inline code, blockquote, unordered/ordered/task list, link, image, code block, horizontal rule, and table. The source's empty-selection placeholders and selected-text wrapping/line prefixes are the behavioral reference. Any change updates the same draft generation and “Unsaved” state as typing; existing autosave handles persistence. In View mode or with no document, editor mutation controls are unavailable.
- Find/replace operates on the current document text only. An empty query clears match count and navigation. A nonempty query shows “0 matches” or current index of total matches. Next/Previous wrap through matches and select the match in the editor. Match Case changes the count and positions. Replace changes the selected match; Replace All changes every match, including safe handling of literal replacement characters. Both update the same draft and match count. No-result actions are disabled or inert with text feedback. Ctrl+F focuses Find, Enter moves next, Shift+Enter moves previous, and Esc closes and returns focus to the editor. Switching files resets match navigation for the new buffer without altering either document.
- The Table dialog preserves the insertion range while open. Clamp or visibly reject values outside 1–10 columns and 1–20 rows; preview updates as values/alignment change. Insert places the preview Markdown at the remembered range, closes, marks the draft dirty, and restores editor focus and caret. Cancel, Close, and Esc make no content change. Invalid input cannot yield an ambiguous table.
- The Templates tab shows all four source cards even when the vault is empty. Selecting one does not overwrite the active document or assume that `Getting Started` exists. The scoped Apply Template dialog offers Root and folders returned by the tree, proposes a filename from the source template title, and lets the owner edit it. Creating with an invalid name, absent parent, offline API, or overlarge content shows an actionable error without losing the current editor draft or dialog input. On success, refresh the tree and open the new document in Edit mode. A `412 PATH_EXISTS` response leaves the existing file unchanged, shows the collision, and asks before replacement. If the owner declines, return to naming/cancel; if the owner accepts, fetch the existing document's current revision and use conditional replacement. A stale follow-up conflict returns to the approved conflict flow. No general New File, rename, delete, or upload dialog is designed here; those remain `TASK-WEB-004`.
- Help is read-only and client-side. Keep the source shortcut map: Ctrl+E mode toggle; Ctrl+B bold; Ctrl+I italic; Ctrl+K link; Ctrl+F find/replace; Ctrl+S save; Tab/Shift+Tab indent/outdent; Ctrl+Q blockquote. The cheat sheet shows raw and rendered examples for headings, emphasis, lists/tasks, quotes, code, links, and tables. Only shortcuts actually implemented in this task or prior tasks may be presented as active. Do not let editor shortcuts run while a dialog or non-editor input has focus.

### Accessibility, responsiveness, and reusable patterns

Reuse the existing toolbar button, active tab, inline error, and save-status patterns. Give icon buttons accessible names and shortcut hints; expose pressed/selected state for Match Case, alignment, and sidebar/help tabs with text or shape as well as emerald color. Keep an always-visible focus indicator. Preserve the owner's textarea focus/caret after formatting, find navigation, and table insertion. The Table, Apply Template, and Help dialogs need associated headings, `aria-modal`, a focus trap, Escape/Cancel, and return focus to their trigger. Announce match count changes, validation errors, create success, and collision through appropriate live regions; collision/overwrite decisions require explicit wording. Dialogs scroll internally on short screens and fit below 768px; at 200% zoom, the page has no horizontal overflow, the ribbon reflows or offers an accessible overflow control, and the table preview scrolls within its box. Honor reduced motion and the existing stone/emerald contrast tokens.

### Contract handoff and review

| Action or visible data | Approved contract | Angular responsibility |
|---|---|---|
| Formatting, find/replace, table generation, help | Client-side only; no new OpenAPI operation | Transform current local text, preserve selection/caret, update dirty generation, and rely on the existing save flow. |
| Destination folders for template creation | `getVaultTree`, `Node[]` | Offer Root and only existing folder paths; do not create an implicit parent or seed files. |
| New document from template | `putDocument` with `If-None-Match: *`, `Document` response, `412 PATH_EXISTS`; if owner confirms replacement, `getDocument` then `putDocument` with quoted `If-Match` | Validate `.md` path, preserve existing/dirty content on failure, and open the returned normalized document only after success. Backend OKF normalization remains authoritative. |
| Save of edited text | Existing `putDocument` conditional update from `TASK-WEB-002` | Do not create a second save path or change the approved wire shape. |

All five Stitch screens were retrieved by screen ID and visually reviewed against the approved source shell and the current Angular editor. Generated data and extra controls were excluded above. The scope covers normal, empty/no-match, invalid, collision, offline/error, cancel, success, keyboard, focus, and responsive states. The approved BRD, application plan, API specification, and contracts remain unchanged. No unresolved contract or architecture conflict remains; **Editor tools is Ready for Angular** for `TASK-WEB-003` only. File mutations/upload, rendered viewer/OKF inspector, export, and preferences still require their own design passes.

## File mutations and upload scope — `TASK-WEB-004`

| Field | Value |
|---|---|
| Scope status | Ready for Angular |
| Date | 2026-09-23 |
| Sources | Approved BRD v1.0 (`FR-VAULT-002`, `FR-VAULT-003`, `FR-VAULT-004`, US-001, US-005); Application Development Plan v1.0; API specification v1.0; OpenAPI v1.0 |
| Stitch project and design system | `projects/9423112390778364876`; `assets/14841597212643379249` |
| Reviewed screens | File/folder actions: `projects/9423112390778364876/screens/7ac7c15e38574d03b92a3b991416712b`; permanent delete: `projects/9423112390778364876/screens/d2d6436744ae4686a8dc27050b3641ef`; upload batch: `projects/9423112390778364876/screens/f2fa86a962bc4ab5bf2b7546fce33628` |
| Angular task | `TASK-WEB-004` only |

### Intent and screen inventory

Keep mutations in the existing workspace shell and explorer. The owner can create a folder, create a Markdown document, rename a file or folder, upload Markdown files into an existing folder, and permanently delete an item. Root and folders already returned by `getVaultTree` are valid destinations; no operation creates implicit parents. The reviewed Stitch screens are composition references only: generated files, counts, localhost labels, settings, cloud controls, and sample metadata are not product data.

| Surface | Entry, behavior, and exit | Stitch reference |
|---|---|---|
| File/folder actions | Contextual actions on the selected tree item and root action bar open compact create, rename, or upload dialogs. Create folder uses a name field; create document uses a safe `.md` filename and content; rename uses the current path and new basename. Cancel leaves the tree and active editor untouched. | `screens/7ac7c15e38574d03b92a3b991416712b` |
| Permanent delete | Delete opens an alertdialog naming the exact file/folder and warning that deletion is permanent. Only an explicit Delete action sends the request; Cancel, Escape, and backdrop dismissal send no request. On success refresh the tree and close an affected editor; on failure retain selection and show the returned error. | `screens/d2d6436744ae4686a8dc27050b3641ef` |
| Upload Markdown batch | Upload opens a dialog with destination folder, keyboard file picker, drop zone, staged file list, per-file status, and remove controls. Show UTF-8 byte total against the 25,000,000-byte batch limit and preflight errors before dispatch. | `screens/f2fa86a962bc4ab5bf2b7546fce33628` |

### Interaction and state rules

- Create folder calls `POST /api/v1/vault/folders` with an existing parent or root. Create document calls `PUT /api/v1/vault/documents` with `If-None-Match: *`; invalid names, missing parents, and `409 PATH_EXISTS` preserve dialog input. A collision requires an explicit review/overwrite decision; replacement first reads the current document and then uses its quoted `If-Match` revision. Rename calls `POST /api/v1/vault/moves` with the source revision and never overwrites a destination. `404`, `409`, `412`, `422`, and `500` remain visible and actionable.
- Delete is permanent and uses `DELETE /api/v1/vault/items` with the selected node's current quoted revision. The confirmation must identify whether the target is a file or folder and state that descendants will be removed for a folder. Declining the prompt performs no destructive call. Refresh after success and keep the owner informed with a live result message.
- Upload accepts `.md`/`text/markdown` files through picker or drop. Reject unsupported files with a per-file reason. Calculate each file's UTF-8 byte length and block the whole batch before network dispatch when the total exceeds 25,000,000 bytes. Existing names show a per-file collision choice; only explicitly approved replacements send the current unquoted revision in `expectedRevision`, while new files send `null`. Send one `POST /api/v1/vault/uploads` request and render every `207` per-file outcome (created, conflict, invalid, too large, or not attempted); never automatically replay a partial batch.
- All dialogs use the existing focus-trap pattern, have labeled controls and headings, return focus to the originating tree/action button, and announce validation, progress, and results through live regions. Drag/drop always has a keyboard-equivalent picker. At mobile widths dialogs scroll internally, the explorer remains the existing drawer, and the page has no horizontal overflow at 200% zoom.

### Contract handoff and review

| Visible action/data | Approved contract | Angular responsibility |
|---|---|---|
| Create folder | `POST /folders`, `FolderCreateRequest`, `201`, `409 PATH_EXISTS` | Validate basename, use only an existing parent, refresh tree after success. |
| Create/rename document or folder | `PUT /documents` with `If-None-Match: *`; `POST /moves` with `If-Match` | Preserve inputs on errors, ask before replacement, and never silently overwrite. |
| Permanent delete | `DELETE /items` with `If-Match`, `204` | Require explicit confirmation, refresh selection, and report failures without retrying destructively. |
| Batch upload | `POST /uploads`, 25,000,000 UTF-8 bytes, `207` per-file results | Preflight all files, collect explicit collision choices, display each outcome, and do not replay partial work. |

The three Stitch screens were generated and reviewed for the approved source shell and contracts. Their illustrative data and extra controls are excluded above. The approved planning documents and contracts remain unchanged; **File mutations and upload is Ready for Angular** for `TASK-WEB-004` only.

## Preview, table of contents, and OKF inspector scope — `TASK-WEB-005`

| Field | Value |
|---|---|
| Scope status | Ready for Angular |
| Date | 2026-09-23 |
| Sources | Approved BRD v1.0 (`FR-EDIT-003`, `FR-OKF-001`, US-003, US-004); Application Development Plan v1.0; API specification v1.0; OpenAPI v1.0; OKF schema |
| Stitch project and design system | `projects/9423112390778364876`; `assets/14841597212643379249` |
| Reviewed screens | Rendered viewer: `projects/9423112390778364876/screens/5364bf89890e47ad8e52ca5ac1927093`; metadata inspector: `projects/9423112390778364876/screens/a3a22006d8c04539abb9ef3c66e522a3` |
| Angular task | `TASK-WEB-005` only |

### Intent and screen inventory

View mode replaces the editor content in the existing workspace route while preserving the explorer, document identity, mode toggle, save state, and local status bar. Render Markdown safely with headings, anchored navigation, tables, links, fenced code with copy controls, and interactive task checkboxes. The table of contents is derived from headings in the current document and scrolls to the corresponding rendered heading. A metadata inspector is available from the editor/viewer toolbar and edits the current draft locally before the existing conditional save flow persists it.

| Surface | Entry, behavior, and exit | Stitch reference |
|---|---|---|
| Rendered viewer | Select View or Ctrl+E. Show sanitized Markdown output, heading anchors, TOC, code-copy actions, tables, links, and task controls. Switching back to Edit preserves the exact draft. | `screens/5364bf89890e47ad8e52ca5ac1927093` |
| Table of contents | List H1–H6 headings in document order, indent by level, highlight the visible heading, and focus/scroll to the target on activation. Empty headings are omitted. | `screens/5364bf89890e47ad8e52ca5ac1927093` |
| OKF metadata inspector | Open a labeled panel/dialog from the document toolbar. Parse supported frontmatter fields, show valid/invalid state, preserve unknown keys, and provide Apply/Conform and Cancel. Apply changes the editor draft only; save remains explicit/autosaved through `TASK-WEB-002`. | `screens/a3a22006d8c04539abb9ef3c66e522a3` |

### Interaction and state rules

- Render with an XSS-safe Markdown pipeline. Do not execute raw HTML or unsafe URL schemes. Preserve fenced-code language labels and provide a keyboard-accessible Copy Code action with a live success/failure announcement. Tables retain headers and alignment. External links use safe target/rel behavior and clearly indicate navigation.
- Each task checkbox maps to the corresponding Markdown task marker in the draft. Toggle only the marker state, update the draft generation, and show the normal Unsaved/autosave state. A task interaction in View must not mutate a different document after a file switch or stale render.
- Build the TOC from rendered heading IDs. Clicking a TOC item moves focus to and scrolls the matching heading; keyboard activation is equivalent. Missing or duplicate headings receive deterministic unique IDs. The TOC remains usable when no headings exist and reports an empty state.
- Parse the source OKF v0.2 frontmatter subset locally. A document is valid only when `type` is nonempty; show validity and field-level errors without discarding content. Supported fields may be edited with schema-aligned controls. Preserve unknown YAML keys and unsupported content verbatim when rebuilding frontmatter. Conform normalizes only supported fields and writes into the current draft; it does not issue a new endpoint or silently save.
- Invalid YAML/frontmatter keeps the raw editor text intact, marks the inspector invalid, and blocks Conform until corrected. Cancel closes without draft changes. Apply/Conform returns focus to the invoking control and announces the draft update. Save errors and conflicts continue to use the existing editor/store recovery flow.
- The viewer and inspector use visible focus, semantic headings, keyboard operation, live status announcements, and internal scrolling. At mobile widths the explorer remains the existing drawer, the TOC can collapse into a compact panel, metadata fields stack, and the page has no horizontal overflow at 200% zoom.

### Contract handoff and review

| Visible action/data | Approved contract | Angular responsibility |
|---|---|---|
| Read/render document | Existing `getDocument` / `Document` content | Parse and sanitize locally; never change the API payload or invent a rendered endpoint. |
| Task toggle and metadata edits | Existing `putDocument` through the store with conditional `If-Match` | Update the same draft generation, preserve unknown content, and let the approved save/conflict flow persist. |
| OKF validity | `contracts/schemas/okf-frontmatter.schema.json` and normalizer behavior | Require nonempty `type`, show supported validation, and retain unknown keys through round trips. |

Both Stitch screens were generated and reviewed against the approved source shell, BRD, API specification, and OKF schema. Generated sample filenames, counts, localhost labels, security badges, and extra export/settings controls are illustrative and excluded from implementation. No upstream artifact or contract changes are required; **Preview, table of contents, and OKF inspector is Ready for Angular** for `TASK-WEB-005` only.

## Export, status, and preferences scope — `TASK-WEB-006`

| Field | Value |
|---|---|
| Scope status | Ready for Angular |
| Date | 2026-09-23 |
| Sources | Approved BRD v1.0 (`FR-EXPORT-001`, FR-WORKSPACE-001, US-007); Application Development Plan v1.0; API specification v1.0; OpenAPI v1.0 |
| Stitch project and design system | `projects/9423112390778364876`; `assets/14841597212643379249` |
| Reviewed screens | Export/status/preferences: `projects/9423112390778364876/screens/60022c4460644255851e149f56f5137c` |
| Angular task | `TASK-WEB-006` only |

### Intent and screen inventory

Keep exports and local preferences in the existing workspace toolbar and status surfaces. Export actions operate on the current draft content and rendered viewer output without a backend endpoint. Status shows document metrics and save state in words. Preferences are explicitly local browser settings and restore after reload.

| Surface | Entry, behavior, and exit | Stitch reference |
|---|---|---|
| Export menu/dialog | Toolbar Export opens actions for Download Markdown, Download rendered HTML, Copy rendered HTML, and browser Print. Show filename derived from the active document and a dismissible success/error announcement. | `screens/60022c4460644255851e149f56f5137c` |
| Status/statistics | Existing bottom status strip and editor header show words, characters, lines, cursor position, UTF-8, and Saved/Unsaved/Saving/Conflict state. Counts update from the current draft without network calls. | `screens/60022c4460644255851e149f56f5137c` |
| Preferences | Local panel for theme (dark/light/system), editor font size, word wrap, and sidebar visibility. Changes apply immediately, persist to localStorage, and restore on reload. | `screens/60022c4460644255851e149f56f5137c` |

### Interaction and state rules

- Download Markdown uses the current complete draft, preserves frontmatter, and names the file from the active basename with `.md`. Rendered HTML uses the same sanitized viewer output and a standalone document shell. Copy rendered HTML uses the Clipboard API with a visible fallback/error. Print invokes the browser print flow with print styles; no server export route is added.
- Every export action has an accessible name, keyboard activation, and a polite live announcement. Close menus on Escape and return focus to the Export trigger. Export failures retain the document and show an actionable message.
- Statistics derive from the current draft: word count, character count, line count, cursor line/column where applicable, UTF-8, and current save state. Counts must not use illustrative Stitch values and must update after edits, task toggles, metadata changes, and file switches.
- Preferences use a versioned localStorage key, validate values on load, and fall back to approved defaults if storage is absent or malformed. Theme and font changes honor reduced motion and visible focus. Sidebar visibility must remain keyboard reachable when collapsed; mobile layout uses the existing drawer and never introduces page-level horizontal overflow.

### Contract handoff and review

| Visible action/data | Approved contract | Angular responsibility |
|---|---|---|
| Markdown/HTML export, copy, print | Client-side only; no new API operation | Use current draft/viewer content, safe filenames, sanitized HTML, browser download/clipboard/print primitives, and feedback states. |
| Save status and statistics | Existing `DocumentStore` and `Document` data | Derive metrics locally and preserve the existing conditional save/conflict semantics. |
| Theme/font/wrap/sidebar preferences | Local browser storage only | Validate, persist, restore, and apply approved settings without cloud or backend state. |

The Stitch screen was generated and reviewed against the approved shell and requirements. Its sample filenames, counts, localhost labels, and decorative controls are illustrative and excluded from implementation. No upstream artifact or contract changes are required; **Export, status, and preferences is Ready for Angular** for `TASK-WEB-006` only.
