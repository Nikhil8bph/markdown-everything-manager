# Business Requirement Document — Markdown FE/BE Everything

| Field | Value |
|---|---|
| Status | Approved |
| Version | 1.1 |
| Date | 2026-09-24 |
| Sources | Approved BRD v1.0; `../../Markdown-Everything` application; user's MCP choices and implementation request on 2026-09-24 |
| Approval evidence | The user approved v1.0 on 2026-09-23, selected the MCP scope recorded below on 2026-09-24, and explicitly approved this v1.1 revision on 2026-09-24. |

## Executive summary

Rebuild the existing MarkCraft Markdown workspace in `markdown-fe-be-everything` with an Angular frontend and a Spring Boot backend. The approved baseline serves one trusted user, stores `.md` documents in a server-side folder vault, and preserves the source application's visual layout and user workflows. The proposed addition lets local AI agent systems manage that vault through MCP while the backend runs. Success means both browser and authorized agent operations stay inside the configured vault, preserve file integrity, and return clear conflict or failure results.

This is a functional migration of the source app, not a new multiuser product. The source Angular/Express application is the behavior and visual reference; the new target is the delivery location. No existing vault is available in the source checkout, and this release starts with an empty vault.

## Users and scope

**Primary persona — workspace owner:** A single trusted user who maintains a personal or private collection of Markdown knowledge documents. They need quick navigation, editing, metadata inspection, and portable exports. There are no distinct end-user roles in this release.

**Secondary persona — local AI agent client:** A program configured by the workspace owner to inspect and manage Markdown documents through MCP. It acts under the owner's authority and has no separate account or folder-specific role.

| Priority | Capability |
|---|---|
| Must | Folder and document browsing, create/open/save/rename/delete, multi-file upload, edit and rendered view modes, OKF metadata, Markdown formatting, search/replace, export, recoverable save errors, and opt-in local MCP access to existing vault actions. |
| Should | Source-equivalent templates, table builder, table of contents, document statistics, keyboard shortcuts, themes, font size, and layout preferences. These are included in the requested full-parity release. |
| Could | Usability fixes discovered during parity verification, provided they do not change approved business behavior. |
| Won't in this release | Accounts and roles, collaborative editing, database storage, import of an existing vault, mobile or desktop native apps, built-in AI generation, cross-document search for agents, remote MCP access, per-client folder restrictions, an MCP settings screen, and Git versioning. |

## Functional requirements

| ID | Priority | Requirement and observable result | Source reference |
|---|---|---|---|
| FR-VAULT-001 | Must | Show a recursive tree of folders and `.md` files, with folder/file names, paths, modification time, and file sizes where available. Let the user filter and navigate it. | `DocumentStore`, `Sidebar`, `FolderDashboard` |
| FR-VAULT-002 | Must | Create folders and `.md` documents in the vault; new documents receive OKF-compatible frontmatter and open for editing. Ask before replacing a document with the same path. | `DocumentStore.createFolder/createFile`; user collision decision |
| FR-VAULT-003 | Must | Open, rename, and permanently delete files or folders after visible confirmation. Folder deletion includes its descendants. Refresh navigation after each successful change. | `DocumentStore`, `Sidebar`, `FolderDashboard`; user deletion decision |
| FR-VAULT-004 | Must | Upload one or more local text/Markdown files through file selection or drag and drop into the chosen folder, normalize their content to the supported OKF format, and show the resulting documents. Ask before replacing any document with the same path. Reject batches over 25 MB with clear feedback. | `DocumentStore.uploadFiles`, `FolderDashboard`; user collision and size decisions |
| FR-EDIT-001 | Must | Edit raw Markdown and switch between Edit and View modes without losing the current document or content. Provide manual save and the source's approximately one-second debounced autosave behavior. | `App`, `DocumentStore`, `EditorPane` |
| FR-EDIT-002 | Must | Provide the source formatting actions, including headings, emphasis, lists, tasks, links, images, code, quotes, rules, and generated tables. Support in-document find/replace and case matching. | `Toolbar`, `EditorPane`, `TableDialog` |
| FR-EDIT-003 | Must | Render Markdown with the source's syntax highlighting, tables, links, code copy, clickable task-list controls, and heading navigation. | `ViewerPane`, `Sidebar` |
| FR-OKF-001 | Must | Parse and display the source app's OKF v0.2 frontmatter subset. Require a nonempty `type` for a document to display as valid; allow the user to update supported metadata and conform an existing document. | `okf-parser`, `EditorPane`, `ViewerPane` |
| FR-EXPORT-001 | Must | Download the current document as `.md` or rendered `.html`, and provide the source's print and copy-rendered-HTML actions. | `DocumentStore`, `Toolbar`, `ViewerPane` |
| FR-WORKSPACE-001 | Should | Show document statistics, cursor position, saved/dirty state, file and folder summaries, templates, and the help/shortcut dialog. | `StatusBar`, `FolderDashboard`, `HelpDialog`, templates |
| FR-WORKSPACE-002 | Should | Preserve view mode, theme, and font-size preferences across browser reloads. Support word wrap and sidebar toggles during a session. | `DocumentStore`, `Toolbar`, `EditorPane` |
| FR-ERROR-001 | Must | Show actionable failures for loading and mutating vault content. A failed or superseded save must not falsely mark newer edits saved; the user must be able to retry without losing the current text. | Migration safety requirement from approved plan |
| FR-BOUNDARY-001 | Must | Restrict all file operations to the configured vault and supported Markdown file type; reject attempts to address files outside it. | Migration safety requirement from approved plan |
| FR-MCP-001 | Must | Expose a local MCP connection while the backend is running, including when the browser is closed. Keep it disabled until the owner explicitly enables it. | User's local HTTP and availability choices |
| FR-MCP-002 | Must | Require an owner-configured token for every MCP client connection and reject missing or invalid credentials without exposing vault data or performing an action. | User's client credential choice |
| FR-MCP-003 | Must | Let an authorized agent list the vault and read Markdown documents throughout the configured vault. | User's full-vault and full-management choices |
| FR-MCP-004 | Must | Let an authorized agent create and update documents, create folders, upload Markdown, and move or rename files and folders without a per-action owner prompt. Apply the same vault limits, normalization, collision protection, and revision checks as browser operations. | User's full-management and preauthorization choices |
| FR-MCP-005 | Must | Let an authorized agent permanently delete files or folders only when the owner has separately enabled agent deletion. Keep agent deletion disabled by default, and apply the same vault boundary and revision protection as browser deletion. | User's separate deletion-toggle choice |
| FR-MCP-006 | Must | Provide a copyable local MCP client configuration and setup instructions without adding a settings screen. | User's documented-config choice |

## User stories and acceptance criteria

### US-001 — Organize a vault

As the workspace owner, I want to create and navigate folders and documents so that my knowledge stays organized.

- **Given** an empty vault, **when** I open the app, **then** I see an empty state and can create a folder or document.
- **Given** a folder and a document in it, **when** I browse or filter the tree, **then** I can find and open that document at its relative path.
- **Given** a selected item, **when** I rename it, **then** the tree shows its new name and the open document remains reachable.
- **Given** a document already exists at a requested create or upload path, **when** I submit the new document, **then** the app asks before overwriting; declining leaves the existing content unchanged.
- **Given** a folder with documents, **when** I confirm deletion, **then** the folder and descendants are permanently removed from the vault and tree.

### US-002 — Edit and save

As the workspace owner, I want my Markdown edits saved so that I can resume work after reopening the app.

- **Given** an open document, **when** I type, **then** the editor marks it dirty and autosave begins after roughly one second of inactivity.
- **Given** an open document, **when** I use the save command, **then** the latest editor content is persisted and reopening the document returns it.
- **Given** a save failure, **when** the request finishes, **then** the editor retains the unsaved text, shows the failure, and permits retry.
- **Given** overlapping saves, **when** an older response arrives after a newer edit, **then** the newer edit is not marked saved or replaced.

### US-003 — Preview and navigate

As the workspace owner, I want a readable preview and heading navigation so that I can review long documents.

- **Given** Markdown with headings, code, a table, and tasks, **when** I switch to View, **then** the preview renders them and displays the source-equivalent navigation and controls.
- **Given** a heading in the table of contents, **when** I select it, **then** the preview scrolls to that heading.
- **Given** a task checkbox in View, **when** I toggle it, **then** its Markdown state changes and remains eligible for save.

### US-004 — Work with OKF metadata

As the workspace owner, I want to inspect and edit document metadata so that my notes retain the source app's knowledge format.

- **Given** Markdown without OKF frontmatter, **when** I create or upload it, **then** the stored document has the supported frontmatter with a nonempty `type`.
- **Given** a document with supported OKF fields, **when** I edit metadata, **then** the updated metadata appears in the editor and preview and persists after save.
- **Given** a document missing `type`, **when** I open it, **then** the app indicates its invalid state and offers the source's conform action.

### US-005 — Import and export portable documents

As the workspace owner, I want to upload and export Markdown so that I can move content in and out of the vault.

- **Given** multiple supported local files, **when** I choose or drop them into a folder, **then** each successful upload appears there and can be opened.
- **Given** a selection totaling more than 25 MB, **when** I attempt an upload, **then** the app rejects the batch and explains the limit without writing any of its files.
- **Given** an open document, **when** I export Markdown or HTML, **then** the browser downloads the current document in the selected format.

### US-006 — Use editing aids

As the workspace owner, I want formatting, search, templates, and shortcuts so that editing stays efficient.

- **Given** selected text, **when** I choose a formatting action, **then** the expected Markdown syntax is applied at the selection.
- **Given** matches in the current document, **when** I find, replace one, or replace all, **then** the content and match count reflect the chosen operation.
- **Given** a template, **when** I apply it, **then** a new editable Markdown document is created from its content.
- **Given** saved UI preferences, **when** I reload the browser, **then** view mode, theme, and font size return to their saved values.

### US-007 — Keep vault access bounded

As the workspace owner, I want the backend to reject paths outside the vault so that a document operation cannot modify unrelated server files.

- **Given** a path containing traversal or an equivalent escape, **when** a file or folder operation is requested, **then** it is rejected and no outside file is read, written, renamed, or deleted.

### US-008 — Connect a local agent

As the workspace owner, I want to connect my local AI agent to MarkCraft so that it can work with my vault while the backend is running.

- **Given** a default installation, **when** an MCP client attempts to connect, **then** access is unavailable until I enable MCP.
- **Given** MCP is enabled and the backend is running, **when** a local client connects with the configured token, **then** it can discover the available vault actions even if the browser is closed.
- **Given** an absent or invalid token, **when** a client attempts any MCP action, **then** it cannot read or change vault contents.
- **Given** the setup guide, **when** I configure a supported local MCP client, **then** I can connect without using a MarkCraft settings screen.

### US-009 — Manage documents through an agent

As the workspace owner, I want my authorized agent to organize and edit Markdown so that it can carry out knowledge-work tasks in my vault.

- **Given** an authorized client, **when** it lists and reads the vault, **then** it receives the same files and persisted document content available to the browser.
- **Given** an authorized client, **when** it creates, updates, uploads, moves, or renames a vault item, **then** the result is visible through the browser and follows the existing file rules without a per-action prompt.
- **Given** an occupied destination or stale revision, **when** the agent attempts a change, **then** the existing content remains intact and the client receives a conflict result.
- **Given** a path outside the vault or an unsupported file type, **when** the agent attempts an operation, **then** it is rejected without changing files outside the allowed boundary.

### US-010 — Control agent deletion

As the workspace owner, I want a separate control over permanent agent deletion so that general agent write access does not automatically include it.

- **Given** MCP access is enabled and agent deletion remains off, **when** an authorized client requests deletion, **then** the item remains unchanged.
- **Given** I separately enable agent deletion, **when** an authorized client requests deletion with the current revision, **then** the item is permanently deleted without a per-action prompt.

## Non-functional requirements and constraints

| ID | Requirement |
|---|---|
| NFR-001 | Deliver a browser-based Angular frontend and Spring Boot backend. Keep the source app's layout and interactions recognizable. |
| NFR-002 | Optimize for one trusted user and localhost-only access. Browser accounts, tenant isolation, and concurrent collaboration are outside this release; MCP access requires the configured client token. |
| NFR-003 | Store documents as UTF-8 `.md` files under a configurable server-side vault directory; start empty and do not import source files. |
| NFR-004 | Protect file integrity across failed saves; report failures in the UI. Validate all backend paths at the filesystem boundary, including equivalent or indirect escape forms. |
| NFR-005 | Preserve keyboard operation for the source's supported shortcuts and provide accessible labels, focus behavior, and readable error feedback in the adapted Angular UI. |
| NFR-006 | Keep the original source checkout unchanged. Place project documents under this target's `docs/`, machine-readable contracts under `contracts/`, and follow the approved stage-2 topology during implementation. |
| NFR-007 | Keep the MCP connection on loopback, disabled by default, and available only while the existing backend runs. Reject unauthenticated MCP calls and prevent local web pages from gaining access through browser-origin or host manipulation. |
| NFR-008 | Apply existing file-integrity and vault-containment rules equally to browser and MCP operations. Keep agent deletion separately disabled by default. |

No numerical latency, capacity, availability, retention, or recovery target was supplied. Those values must be agreed before the architecture stage sets operational guarantees.

## Assumptions, open questions, and risks

| ID | Type | Item | Impact / proposed resolution |
|---|---|---|
| A-001 | Confirmed | Full source feature parity, preserved visual design, server filesystem storage, one trusted user, no existing-vault import. | Basis of this draft. |
| A-002 | Confirmed | Ask before overwriting a document when creation or upload collides with an existing path. | User answered Q-001; source currently overwrites without asking. |
| A-003 | Confirmed | Deletion is permanent after confirmation; there is no trash in this release. | User answered Q-002. |
| A-004 | Confirmed | The maximum upload batch size is 25 MB; reject an oversized batch before writing any files. | User answered Q-003. |
| A-005 | Confirmed | The first release is reachable on localhost only. | User answered Q-004. |
| A-006 | Confirmed | MCP serves local clients over HTTP while the backend runs; the browser need not be open. | User selected local HTTP and backend-running availability. |
| A-007 | Confirmed | An owner-configured token is required; MCP and agent deletion are separately disabled by default. Agent mutations are preauthorized when enabled. | User selected these access and deletion policies. |
| A-008 | Confirmed | Authorized agents access the whole vault and its existing management actions. The release adds a documented client configuration, without a settings screen, cross-document search, or built-in AI workflow. | User selected full-vault management and documented setup. |
| R-001 | Risk | The source's path check uses a string prefix and does not address every path or symlink escape. | Specify safe vault containment in architecture and test traversal and symlink cases. |
| R-002 | Risk | Source save failures are logged without UI feedback, and overlapping saves can clear dirty state. | Make recoverable errors and current-content save state acceptance criteria. |
| R-003 | Risk | The source's frontend and backend use separate lightweight OKF normalization logic. | Stage 3 must define one observable storage rule and tests for metadata round trips. |
| R-004 | Risk | “OKF v0.2” in the source is a local parser/serializer subset; external conformance was not established. | Preserve the observed subset; do not claim broader standard compliance without a supplied specification. |
| R-005 | Risk | A token or enabled local endpoint could be exposed to unintended local software or web pages. | Stage 2 must define token handling and loopback/browser-origin protections; stage 3 must specify rejection behavior. |
| R-006 | Risk | Browser and agent edits may overlap, and agent deletion is permanent. | Reuse revision preconditions, require a separate deletion setting, and cover conflicts and deletion policy in acceptance checks. |

## Stage gate

The v1.0 baseline was confirmed by the user on 2026-09-23, and this v1.1 MCP revision was confirmed on 2026-09-24. Its new requirements affect the approved application plan, API specification and contracts, and implementation strategy; revise each in workflow order. No MCP implementation task may begin from the v1.0 downstream artifacts.
