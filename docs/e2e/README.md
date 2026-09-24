# End-to-End (E2E) Test Suite Documentation

Welcome to the **MarkCraft E2E Testing Suite** documentation directory.

## Versions & Test Reports

| Version | Date | Status | Report Link | Highlights |
| :---: | :---: | :---: | :--- | :--- |
| **`v2.0.0`** | 2026-09-24 | **PASSED** | [`e2e-test-report-v2.0.0.md`](./e2e-test-report-v2.0.0.md) | Fresh packaged app and MCP SDK E2E; 15 scenario groups; 74 frontend and 56 backend tests; MCP-to-browser persistence, security boundaries, and desktop/mobile evidence. |
| **`v1.0.0`** | 2026-09-23 | **PASSED** | [`e2e-test-report-v1.0.0.md`](./e2e-test-report-v1.0.0.md) | Comprehensive 18-scenario test suite run via Chrome MCP server against packaged JAR on `127.0.0.1:8080`. 24 screenshots, 28 network requests audited. |

## v2.0.0 Screenshot Evidence

- [`00-empty-launch.png`](./screenshots-v2/00-empty-launch.png) — fresh empty vault and packaged workspace
- [`01-mcp-to-browser.png`](./screenshots-v2/01-mcp-to-browser.png) — MCP-created Markdown opened in the browser
- [`02-app-editor-e2e.png`](./screenshots-v2/02-app-editor-e2e.png) — browser-edited and persisted Markdown
- [`03-rendered-preview-toc.png`](./screenshots-v2/03-rendered-preview-toc.png) — rendered Markdown, task, table, and table of contents
- [`04-mobile-drawer.png`](./screenshots-v2/04-mobile-drawer.png) — responsive explorer drawer at 390px

## v1.0.0 Screenshot Evidence

Captured visual evidence across all testing flows is archived in:
[`docs/e2e/screenshots-v1/`](./screenshots-v1/)

1. [`01-initial-empty-vault.png`](./screenshots-v1/01-initial-empty-vault.png) — Clean initial vault state & dashboard
2. [`02-create-folder-modal.png`](./screenshots-v1/02-create-folder-modal.png) — Folder creation modal
3. [`03-folder-created.png`](./screenshots-v1/03-folder-created.png) — Vault explorer with newly created folder
4. [`04-create-document-modal.png`](./screenshots-v1/04-create-document-modal.png) — Markdown file creation modal
5. [`05-document-opened-editor.png`](./screenshots-v1/05-document-opened-editor.png) — Automatic opening of new document in raw editor
6. [`06-editing-content-and-frontmatter.png`](./screenshots-v1/06-editing-content-and-frontmatter.png) — Active editing and Unsaved status transition
7. [`07-autosave-and-saved-state.png`](./screenshots-v1/07-autosave-and-saved-state.png) — Save execution & disk synchronization
8. [`08-find-replace-panel.png`](./screenshots-v1/08-find-replace-panel.png) — Find & replace panel with match counters
9. [`10-table-generator-dialog.png`](./screenshots-v1/10-table-generator-dialog.png) — Interactive table builder modal
10. [`11-table-inserted.png`](./screenshots-v1/11-table-inserted.png) — Table markdown inserted and saved
11. [`12-template-picker-dialog.png`](./screenshots-v1/12-template-picker-dialog.png) — Built-in OKF v0.2 template cards
12. [`13-document-from-template.png`](./screenshots-v1/13-document-from-template.png) — Technical Guide generated inside nested folder
13. [`14-view-mode-markdown-preview.png`](./screenshots-v1/14-view-mode-markdown-preview.png) — Prism.js syntax highlighted preview
14. [`15-interactive-task-checkbox-toggled.png`](./screenshots-v1/15-interactive-task-checkbox-toggled.png) — Preview-pane task checklist auto-save
15. [`16-table-of-contents-navigation.png`](./screenshots-v1/16-table-of-contents-navigation.png) — Heading navigation via TOC
16. [`17-okf-metadata-inspector.png`](./screenshots-v1/17-okf-metadata-inspector.png) — OKF v0.2 structured metadata modal
17. [`18-rename-file-dialog.png`](./screenshots-v1/18-rename-file-dialog.png) — Vault item rename dialog
18. [`19-file-renamed.png`](./screenshots-v1/19-file-renamed.png) — Renamed file in tree & filesystem
19. [`20-upload-markdown-dialog.png`](./screenshots-v1/20-upload-markdown-dialog.png) — Batch file upload modal
20. [`21-file-uploaded-success.png`](./screenshots-v1/21-file-uploaded-success.png) — Successful upload results banner
21. [`22-preferences-dialog.png`](./screenshots-v1/22-preferences-dialog.png) — Preferences modal (Theme, Font, Word Wrap)
22. [`23-dark-light-theme-switched.png`](./screenshots-v1/23-dark-light-theme-switched.png) — Application shell rendered in Light mode
23. [`24-delete-confirmation-dialog.png`](./screenshots-v1/24-delete-confirmation-dialog.png) — Permanent deletion confirmation prompt
24. [`25-item-deleted-vault-tree.png`](./screenshots-v1/25-item-deleted-vault-tree.png) — Vault tree following item purge
25. [`26-mobile-viewport-drawer.png`](./screenshots-v1/26-mobile-viewport-drawer.png) — Mobile viewport (390px) off-canvas drawer
