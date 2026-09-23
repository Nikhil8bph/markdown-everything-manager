# End-to-End (E2E) Test Suite Documentation

Welcome to the **MarkCraft E2E Testing Suite** documentation directory.

## Versions & Test Reports

| Version | Date | Status | Report Link | Highlights |
| :---: | :---: | :---: | :--- | :--- |
| **`v1.0.0`** | 2026-09-23 | **PASSED** | [`e2e-test-report-v1.0.0.md`](./e2e-test-report-v1.0.0.md) | Comprehensive 18-scenario test suite run via Chrome MCP server against packaged JAR on `127.0.0.1:8080`. 24 screenshots, 28 network requests audited. |

## Screenshot Directory

Captured visual evidence across all testing flows is archived in:
[`docs/e2e/screenshots/`](./screenshots/)

1. [`01-initial-empty-vault.png`](./screenshots/01-initial-empty-vault.png) — Clean initial vault state & dashboard
2. [`02-create-folder-modal.png`](./screenshots/02-create-folder-modal.png) — Folder creation modal
3. [`03-folder-created.png`](./screenshots/03-folder-created.png) — Vault explorer with newly created folder
4. [`04-create-document-modal.png`](./screenshots/04-create-document-modal.png) — Markdown file creation modal
5. [`05-document-opened-editor.png`](./screenshots/05-document-opened-editor.png) — Automatic opening of new document in raw editor
6. [`06-editing-content-and-frontmatter.png`](./screenshots/06-editing-content-and-frontmatter.png) — Active editing and Unsaved status transition
7. [`07-autosave-and-saved-state.png`](./screenshots/07-autosave-and-saved-state.png) — Save execution & disk synchronization
8. [`08-find-replace-panel.png`](./screenshots/08-find-replace-panel.png) — Find & replace panel with match counters
9. [`10-table-generator-dialog.png`](./screenshots/10-table-generator-dialog.png) — Interactive table builder modal
10. [`11-table-inserted.png`](./screenshots/11-table-inserted.png) — Table markdown inserted and saved
11. [`12-template-picker-dialog.png`](./screenshots/12-template-picker-dialog.png) — Built-in OKF v0.2 template cards
12. [`13-document-from-template.png`](./screenshots/13-document-from-template.png) — Technical Guide generated inside nested folder
13. [`14-view-mode-markdown-preview.png`](./screenshots/14-view-mode-markdown-preview.png) — Prism.js syntax highlighted preview
14. [`15-interactive-task-checkbox-toggled.png`](./screenshots/15-interactive-task-checkbox-toggled.png) — Preview-pane task checklist auto-save
15. [`16-table-of-contents-navigation.png`](./screenshots/16-table-of-contents-navigation.png) — Heading navigation via TOC
16. [`17-okf-metadata-inspector.png`](./screenshots/17-okf-metadata-inspector.png) — OKF v0.2 structured metadata modal
17. [`18-rename-file-dialog.png`](./screenshots/18-rename-file-dialog.png) — Vault item rename dialog
18. [`19-file-renamed.png`](./screenshots/19-file-renamed.png) — Renamed file in tree & filesystem
19. [`20-upload-markdown-dialog.png`](./screenshots/20-upload-markdown-dialog.png) — Batch file upload modal
20. [`21-file-uploaded-success.png`](./screenshots/21-file-uploaded-success.png) — Successful upload results banner
21. [`22-preferences-dialog.png`](./screenshots/22-preferences-dialog.png) — Preferences modal (Theme, Font, Word Wrap)
22. [`23-dark-light-theme-switched.png`](./screenshots/23-dark-light-theme-switched.png) — Application shell rendered in Light mode
23. [`24-delete-confirmation-dialog.png`](./screenshots/24-delete-confirmation-dialog.png) — Permanent deletion confirmation prompt
24. [`25-item-deleted-vault-tree.png`](./screenshots/25-item-deleted-vault-tree.png) — Vault tree following item purge
25. [`26-mobile-viewport-drawer.png`](./screenshots/26-mobile-viewport-drawer.png) — Mobile viewport (390px) off-canvas drawer
