# API Contract & Integration Specification — Markdown FE/BE Everything

| Field | Value |
|---|---|
| Status | Approved |
| Version | 1.0 |
| Date | 2026-09-23 |
| Required inputs | Approved BRD v1.0 and Application Development Plan v1.0 |
| Machine-readable contracts | `contracts/openapi/v1.yaml`; `contracts/schemas/okf-frontmatter.schema.json` |
| Approval evidence | User confirmed this specification and `contracts/openapi/v1.yaml` on 2026-09-23 for implementation planning and UX design. |

## Contract boundary

The Angular browser client calls one same-origin REST API at `/api/v1/vault`. The packaged application listens on `127.0.0.1`; there are no accounts, bearer tokens, roles, external integrations, or messaging. All routes are unauthenticated within that localhost boundary. No per-user or endpoint rate limits are required for the approved single-user deployment; the 25,000,000-byte upload batch cap and same maximum UTF-8 document size bound request resources. The frontend handles Markdown rendering, templates, statistics, preferences, print, copy, and downloads locally; those actions do not create API routes.

`contracts/openapi/v1.yaml` is the wire authority. The table below traces routes to the BRD; operation IDs and schemas are in OpenAPI.

| Method | Route | Purpose | BRD |
|---|---|---|---|
| GET | `/api/v1/vault/tree` | Recursive folder/Markdown tree | FR-VAULT-001 |
| GET | `/api/v1/vault/documents?path=...` | Read UTF-8 Markdown and revision | FR-VAULT-001, FR-EDIT-001 |
| PUT | `/api/v1/vault/documents?path=...` | Create or conditionally replace one document | FR-VAULT-002, FR-EDIT-001, FR-OKF-001 |
| POST | `/api/v1/vault/folders` | Create one folder | FR-VAULT-002 |
| POST | `/api/v1/vault/uploads` | Submit a bounded batch of documents | FR-VAULT-004 |
| POST | `/api/v1/vault/moves` | Rename a file or folder within the vault | FR-VAULT-003 |
| DELETE | `/api/v1/vault/items?path=...` | Permanently delete a file or folder | FR-VAULT-003 |

## Paths, revisions, and mutation rules

- `path` is a slash-separated, vault-relative path. Each segment must be nonempty and must not be `.` or `..`; backslashes, absolute paths, NUL, control characters, and hidden/special filesystem entries are rejected. File paths must end in `.md` (case insensitive). Folder paths must not end in `.md`. The vault root itself is never an item path. Requests must not follow symlinks or escape the root.
- A file revision is a lowercase SHA-256 hex digest of its stored UTF-8 bytes. A folder revision is a lowercase SHA-256 digest of its sorted descendant relative paths and file revisions. The tree and document responses expose unquoted `revision`; `ETag` and `If-Match` use its quoted HTTP form. Folder creation/deletion/rename changes relevant folder revisions.
- `PUT` with `If-None-Match: *` means create only. Existing paths return `412 PATH_EXISTS`; the UI then shows an overwrite prompt. If accepted, the UI reads the current document and sends its `ETag` with `If-Match` to replace it. `PUT` with `If-Match` means update only; a missing path returns `404`, and a stale revision returns `412 REVISION_CONFLICT`. A successful write returns the normalized persisted content and new revision.
- Upload files use `expectedRevision: null` for create only or a current unquoted revision for a confirmed overwrite. The client prompts for each collision, then submits the selected batch. The server prevalidates names, paths, duplicates within the batch, size, and all revisions before writing any file. A preflight conflict rejects the whole batch with `409` or `412`. Once writing starts, an I/O failure may leave earlier files committed; return `207` with one result per file so the UI can report exactly what happened. No automatic retries of a partially applied batch.
- Move rejects an occupied destination with `409 PATH_EXISTS`; it never overwrites. Move and delete require `If-Match` for the source item. A missing source returns `404`; a stale revision returns `412`. `DELETE` returns `204` with no response body after the UI has shown its confirmation. A folder deletion removes descendants permanently.
- `POST /folders` is create only. An existing folder returns `409 PATH_EXISTS`. Empty parent path means the vault root; an absent parent folder returns `404` rather than creating an implicit hierarchy. Document creation and upload likewise require an existing parent folder.

Example create request and success response:

```http
PUT /api/v1/vault/documents?path=Notes%2Fidea.md
If-None-Match: *
Content-Type: application/json

{"content":"# Idea\n"}
```

```json
{"success":true,"data":{"path":"Notes/idea.md","name":"idea.md","content":"---\ntype: concept\ntitle: \"Idea\"\n---\n\n# Idea\n","updatedAt":"2026-09-23T12:00:00Z","size":44,"revision":"d556a098112a743ccd48f147ba99b94eabbfc30a11ffe3baf15eb835edb7d310"},"timestamp":"2026-09-23T12:00:00Z"}
```

Other route examples use `r` below as shorthand for a current 64-character revision; actual JSON uses the full digest.

| Route | Example request | Example success `data` or result |
|---|---|---|
| `GET /tree` | No body | `[]` for an empty vault, or a recursive array of `Node` objects. |
| `GET /documents?path=Notes%2Fidea.md` | No body | `Document` with `content`, `size`, `updatedAt`, and `revision`; `ETag` quotes the same revision. |
| `POST /folders` | `{"path":"Notes"}` | A folder `Node` with `children: []`. |
| `POST /uploads` | `{"folder":"Notes","files":[{"name":"idea.md","content":"# Idea","expectedRevision":null}]}` | `[{"path":"Notes/idea.md","status":"created","revision":"<r>"}]`; `207` includes `failed` or `notAttempted` results. |
| `POST /moves` | `{"from":"Notes/idea.md","to":"Notes/renamed.md"}` plus `If-Match: "<r>"` | The moved item `Node` at `Notes/renamed.md`. |
| `DELETE /items?path=Notes%2Frenamed.md` | `If-Match: "<r>"`; no body | `204` with no body. |

## OKF content rules

The API stores complete Markdown text, not a parsed metadata DTO. The frontend may parse and edit metadata locally. On create, upload, or save, the backend applies one normalizer:

1. If a well-formed leading YAML mapping has a nonempty string `type`, preserve the supplied bytes unchanged.
2. If a well-formed leading YAML mapping lacks `type`, insert `type: concept` immediately after the opening delimiter, preserving other fields and body text. Replace an empty or nonstring `type` with `type: concept` rather than creating a duplicate key.
3. If there is no leading frontmatter, prepend a minimal `type: concept` and title inferred from the first Markdown H1 or filename stem, followed by the original body.
4. If the leading delimiter has no closing delimiter or the YAML cannot be parsed as a mapping, reject with `422 INVALID_FRONTMATTER`; do not overwrite the existing file.

Unknown valid YAML keys are retained on backend writes. The supported UI fields are described by `contracts/schemas/okf-frontmatter.schema.json`; that schema does not imply compliance with an external OKF standard. Existing files placed manually in the vault may be read even when invalid, allowing the UI to show the source's validation state and conform action.

## Responses, errors, and security

JSON successes except `204` use `{ "success": true, "data": ..., "timestamp": "RFC3339" }`. A partial upload uses `207` with `success: false` and per-file `data`. Errors use `{ "success": false, "error": { "code": "...", "message": "...", "details": [] }, "timestamp": "RFC3339" }`. `details` is always an array of field/problem objects. Unknown request fields are rejected with `400 INVALID_REQUEST`. All timestamps are UTC RFC 3339 strings. Response bodies never include absolute host paths or document contents in errors.

| Status | Code | Meaning / client action |
|---|---|---|
| 400 | `INVALID_REQUEST`, `INVALID_PATH`, `PRECONDITION_REQUIRED` | Correct request, path, or missing conditional header. |
| 404 | `NOT_FOUND` | Refresh tree; target or parent no longer exists. |
| 409 | `PATH_EXISTS`, `DUPLICATE_BATCH_PATH` | Rename/create folder destination or upload preflight collision; ask or choose another name. |
| 412 | `PATH_EXISTS`, `REVISION_CONFLICT` | Create collision or stale revision; read current item and ask before overwriting. |
| 413 | `PAYLOAD_TOO_LARGE` | Reduce file or batch size below 25,000,000 bytes. |
| 422 | `INVALID_FRONTMATTER`, `UNSUPPORTED_FILE` | Fix malformed Markdown metadata or file type. |
| 500 | `STORAGE_FAILURE` | Keep edits dirty and retry after the local storage problem is resolved. |

The backend checks accepted loopback `Host` values on every request and accepted `Origin` values on mutation requests; development uses the same-origin Angular proxy. No CORS wildcard, cookie session, CSRF token, JWT, or Bucket4j policy is introduced. Retrying `GET` is safe. A failed conditional `PUT` or move/delete must first refresh the revision; never blindly retry a stale mutation. A partially applied upload is reported per file and must not be automatically replayed.

## Events and external integrations

None are approved in the architecture. There is no AsyncAPI artifact and no Kafka, payment, notification, email, SMS, or external AI contract for this release.

## Contract checks and stage gate

OpenAPI YAML parsed, all 92 local references resolved, and the OKF JSON Schema validated on 2026-09-23. A full OpenAPI validator was not installed. The user confirmed this document and its machine-readable artifacts on 2026-09-23. Downstream stages treat these artifacts as read-only; revisions return to this contract stage.
