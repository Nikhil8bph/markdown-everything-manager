# Local MCP client setup

MarkCraft exposes a local MCP Streamable HTTP endpoint at `http://127.0.0.1:8080/mcp`. MCP is disabled by default. When enabled, the bearer token grants access to the whole vault and all seven tools; it is not scoped per client. The server binds to loopback and rejects non-loopback hosts and disallowed browser origins.

## Build and start

Build the packaged backend and frontend using the [local runtime guide](operations-local-runtime.md), then start the backend with MCP enabled. Create a fresh token in a private shell and keep it out of source control, command history, screenshots, and logs:

```bash
export MARKCRAFT_MCP_ENABLED=true
export MARKCRAFT_MCP_TOKEN="$(openssl rand -hex 32)"
export MARKCRAFT_MCP_AGENT_DELETION_ENABLED=false
export MARKCRAFT_VAULT_ROOT="$HOME/.markcraft/vault"
java -jar backend/target/markcraft-0.1.0-SNAPSHOT.jar
```

`MARKCRAFT_MCP_TOKEN` must be nonempty when MCP is enabled. The MCP and agent-deletion switches are independent; deletion remains disabled unless `MARKCRAFT_MCP_AGENT_DELETION_ENABLED=true` is deliberately set. Restart the backend after changing these startup settings. If the token is exposed, stop the server, generate a replacement, update the client secret, and restart.

## VS Code Chat agent configuration

Add this to `.vscode/mcp.json`. VS Code prompts for the token as a password input and stores that input for later use; the bearer value is not embedded in the checked-in configuration. The `type: "http"` entry uses VS Code's HTTP Stream transport for the endpoint.

```json
{
  "inputs": [
    {
      "type": "promptString",
      "id": "markcraft-mcp-token",
      "description": "MarkCraft MCP bearer token",
      "password": true
    }
  ],
  "servers": {
    "markcraft": {
      "type": "http",
      "url": "http://127.0.0.1:8080/mcp",
      "headers": {
        "Authorization": "Bearer ${input:markcraft-mcp-token}"
      }
    }
  }
}
```

Use this in a VS Code Chat agent session that supports interactive MCP inputs. VS Code's Agent Host reads its portable MCP file independently and does not resolve interactive `${input:...}` variables; for Agent Host or another MCP client, configure the same Streamable HTTP URL and `Authorization: Bearer <token>` header using that client's secret-input mechanism. See the [VS Code MCP configuration reference](https://code.visualstudio.com/docs/agents/reference/mcp-configuration) for the HTTP server, headers, and password input fields.

After saving the config, start or restart the VS Code MCP server and provide the token when prompted. With the backend disabled, the endpoint returns `404`. Missing or incorrect tokens return `401`; invalid hosts or disallowed browser origins return `400` before an MCP operation runs.

## Safe tool use

- Call `get_vault_tree` to inspect current paths and revisions. Call `get_document` before changing an existing document.
- For a new document, call `put_document` with `expectedRevision: null`. To replace a document, send the current unquoted `revision` returned by `get_document`.
- `upload_documents` uses `expectedRevision: null` per new file and the file's current revision for an explicitly selected replacement. Inspect every `data[]` outcome. A partial batch has `success: false`, includes `created`, `replaced`, `failed`, and/or `notAttempted` results, and must not be replayed automatically.
- `move_item` and `delete_item` require the current revision. A stale revision returns `REVISION_CONFLICT`; read the current item again before deciding what to do.
- `delete_item` is discoverable when disabled but returns `DELETE_DISABLED` without changing the vault. Enabling it permits permanent deletion of files and recursive folder contents. Keep it off unless the owner intentionally grants this capability.

Every failed tool call has `isError: true` and JSON in its text content; it has no `structuredContent`. Successful calls provide both structured output and a text fallback.

## Packaged acceptance check

From the repository root, run:

```bash
./scripts/verify-packaged-mcp.sh
```

The script builds the Angular browser assets and backend jar, starts packaged instances with fresh temporary vaults, connects using the MCP Java SDK's Streamable HTTP client, exercises all seven tools, checks token/host/origin rejection and both deletion-switch settings, and removes the temporary vaults on exit. The enabled-mode vault leaves `MCP-agent-visible.md` for the browser verification. It does not open or automate a browser.

The report under `docs/verification/TASK-MCP-004/README.md` records the packaged client results and the browser check separately. For a manual client run, keep the browser closed while the MCP client reads and edits files; then open `http://127.0.0.1:8080/` and confirm the agent-created Markdown file appears in the tree and opens with the saved text.
