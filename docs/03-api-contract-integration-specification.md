# API Contract & Integration Specification — Markdown FE/BE Everything

| Field | Value |
|---|---|
| Status | Approved |
| Version | 1.1 |
| Date | 2026-09-24 |
| Required inputs | Approved BRD v1.1 and Application Development Plan v1.1 |
| Machine-readable contracts | `contracts/openapi/v1.yaml`; `contracts/schemas/okf-frontmatter.schema.json`; `contracts/mcp/v1-tools.json` |
| Approval evidence | User confirmed the v1.0 specification and existing REST artifacts on 2026-09-23, approved the revised upstream MCP documents on 2026-09-24, and explicitly approved this v1.1 MCP contract on 2026-09-24. |

## Contract boundary

The Angular browser client calls one same-origin REST API at `/api/v1/vault`. The packaged application listens on `127.0.0.1`; the existing REST routes remain unauthenticated within that localhost boundary. The opt-in MCP transport requires an owner-configured bearer token. There are no browser accounts, roles, external AI services, or messaging. No per-user or endpoint rate limits are required for the approved single-user deployment; the 25,000,000-byte upload batch cap and same maximum UTF-8 document size apply to both transports. The frontend handles Markdown rendering, templates, statistics, preferences, print, copy, and downloads locally; those actions do not create API routes.

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

## MCP interface v1

The dedicated MCP endpoint is `/mcp` on the same loopback port as the REST API. It uses Streamable HTTP and the protocol's negotiated JSON-RPC messages. MCP is disabled by default; when disabled, the route is unavailable. Startup properties are `markcraft.mcp.enabled` (default `false`), `markcraft.mcp.token` (required and nonempty when enabled), and `markcraft.mcp.agent-deletion-enabled` (default `false`); the equivalent environment variables use uppercase underscore names. Enabling MCP without a token fails startup. Every request to `/mcp`, including initialization, discovery, reads, notifications, and stream requests, requires `Authorization: Bearer <token>`. Missing or invalid credentials receive HTTP `401` before MCP message handling. Invalid `Host` or nonmatching browser `Origin` receives HTTP `400` before MCP message handling; no cross-origin browser access is granted. An MCP client without an `Origin` header may connect. The token grants whole-vault access; there are no client-specific scopes.

`contracts/mcp/v1-tools.json` is the machine-readable authority for tool names, argument schemas, success schemas, and annotations. Its `$ref` values are local to the catalog and must be resolved into self-contained tool schemas before registration. The tool catalog is stable while the backend runs. MCP exposes tools only: no resources, prompts, sampling, or model invocation. Tool names map one-to-one to existing vault operations:

| Tool | Input summary | Success data | Existing operation | BRD |
|---|---|---|---|---|
| `get_vault_tree` | Empty object | Recursive `Node[]` | `getVaultTree` | FR-MCP-003 |
| `get_document` | Vault-relative `path` | Persisted `Document` | `getDocument` | FR-MCP-003 |
| `put_document` | `path`, complete `content`, `expectedRevision` | Persisted `Document` | `putDocument` | FR-MCP-004 |
| `create_folder` | Vault-relative `path` | Folder `Node` | `createFolder` | FR-MCP-004 |
| `upload_documents` | `folder`, `files[]` with `name`, `content`, `expectedRevision` | Ordered per-file `UploadResult[]` | `uploadDocuments` | FR-MCP-004 |
| `move_item` | `from`, `to`, `expectedRevision` | Moved `Node` | `moveItem` | FR-MCP-004 |
| `delete_item` | `path`, `expectedRevision` | Deleted `path` | `deleteItem` | FR-MCP-005 |

For `put_document` and each uploaded file, `expectedRevision: null` means create only; a 64-character unquoted revision means explicitly replace the version the agent read. No prompt is issued by MarkCraft. A collision or stale revision fails without writing. `move_item` never overwrites its destination. `move_item` and `delete_item` require the current source revision. `delete_item` is discoverable but returns `DELETE_DISABLED` without modifying anything until the separate agent-deletion setting is enabled; that setting is off by default. Folder deletion is permanent and includes descendants. All paths, OKF normalization, size limits, batch preflight rules, and partial-write reporting match the REST rules above.

Successful tools return `isError: false`, an object in `structuredContent` matching the catalog's `outputSchema`, and a JSON serialization of that object in one text content block for clients that do not use structured output. The object uses the existing success envelope (`success`, `data`, `timestamp`); a partial upload returns `isError: false` with `success: false` and ordered per-file results so callers can inspect committed and failed files without replaying the batch. `delete_item` uses a success object with the deleted relative path because an MCP tool result cannot be an empty HTTP `204`.

Expected tool failures return `isError: true` and one text content block containing a JSON object `{ "code": string, "message": string, "details": [] }`, using the existing REST error codes where applicable and `DELETE_DISABLED` for the separate deletion policy. No `structuredContent` is returned for a failed call. Invalid tool arguments return `INVALID_REQUEST`; invalid vault paths return `INVALID_PATH`; absent paths return `NOT_FOUND`; create or destination collisions return `PATH_EXISTS`; duplicate batch paths return `DUPLICATE_BATCH_PATH`; stale revisions return `REVISION_CONFLICT`; oversized content returns `PAYLOAD_TOO_LARGE`; malformed frontmatter returns `INVALID_FRONTMATTER`; unsupported file types return `UNSUPPORTED_FILE`; storage failures return `STORAGE_FAILURE`. Protocol-level malformed JSON-RPC and unknown tools follow MCP errors. Never include the token, absolute host paths, or document content in an error.

The setup guide must show a local Streamable HTTP client configuration with the endpoint and bearer token, plus startup settings for MCP enablement and optional agent deletion. It must tell the owner to keep the token out of source control and logs. No browser settings surface or REST contract change is required.

## Events and external integrations

None are approved in the architecture. There is no AsyncAPI artifact and no Kafka, payment, notification, email, SMS, or external AI contract for this release. MCP is a local client-to-backend integration defined above.

## Contract checks and stage gate

The v1.0 REST OpenAPI YAML parsed, all 92 local references resolved, and the OKF JSON Schema validated on 2026-09-23; a full OpenAPI validator was not installed. On 2026-09-24, the MCP catalog parsed as JSON, all 45 local references resolved, and its catalog and 14 referenced tool schemas passed JSON Schema 2020-12 structural validation. The user confirmed the v1.0 document and REST artifacts on 2026-09-23 and explicitly confirmed this v1.1 MCP revision on 2026-09-24. Stage 4 must revise `docs/04-implementation-strategy.md` and its traceability/task gates before any MCP implementation begins. Downstream stages treat this document and every file under `contracts/` as read-only.
