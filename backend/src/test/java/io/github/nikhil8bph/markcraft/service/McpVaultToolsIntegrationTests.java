package io.github.nikhil8bph.markcraft.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"markcraft.mcp.enabled=true", "markcraft.mcp.token=local-test-token"})
class McpVaultToolsIntegrationTests {

    @TempDir
    static Path temporaryDirectory;

    @DynamicPropertySource
    static void vaultRoot(DynamicPropertyRegistry registry) {
        registry.add("markcraft.vault.root", () -> temporaryDirectory.resolve("vault").toString());
    }

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    @Autowired
    private ObjectMapper objectMapper;
    private String sessionId;

    @BeforeEach
    void initializeProtocolSession() throws Exception {
        HttpResponse<String> response = sendRpc("initialize", 1,
                Map.of("protocolVersion", "2025-11-25", "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "markcraft-tools-test", "version", "1.0")), null);
        assertThat(response.statusCode()).isEqualTo(200);
        sessionId = response.headers().firstValue("Mcp-Session-Id").orElse(null);

        JsonNode listing = rpcResult(sendRpc("tools/list", 2, Map.of(), sessionId));
        var toolNames = new ArrayList<String>();
        for (JsonNode tool : listing.path("tools")) {
            toolNames.add(tool.path("name").asText());
        }
        assertThat(toolNames)
                .containsExactlyInAnyOrder("get_vault_tree", "get_document", "put_document", "create_folder",
                        "upload_documents", "move_item", "delete_item");
        for (JsonNode tool : listing.path("tools")) {
            assertThat(tool.path("inputSchema").path("$defs").isObject()).isTrue();
            assertThat(tool.path("outputSchema").path("$defs").isObject()).isTrue();
            assertThat(tool.path("inputSchema").has("$ref")).isFalse();
            assertThat(tool.path("outputSchema").has("$ref")).isFalse();
        }
    }

    @Test
    void toolsListReadCreateUpdateAndPreserveContentOnConflict() throws Exception {
        JsonNode emptyTree = callTool("get_vault_tree", Map.of());
        assertThat(emptyTree.path("success").asBoolean()).isTrue();
        assertThat(emptyTree.path("data").isArray()).isTrue();
        assertThat(emptyTree.path("data").size()).isZero();

        JsonNode folder = callTool("create_folder", Map.of("path", "notes"));
        assertThat(folder.path("data").path("path").asText()).isEqualTo("notes");
        assertThat(folder.path("data").path("type").asText()).isEqualTo("folder");

        JsonNode created = callToolWithNullRevision();
        assertThat(created.path("success").asBoolean()).isTrue();
        JsonNode createdDocument = created.path("data");
        String originalRevision = createdDocument.path("revision").asText();
        assertThat(createdDocument.path("content").asText()).contains("# First draft");
        assertThat(createdDocument.path("content").asText()).contains("type: concept");

        JsonNode read = callTool("get_document", Map.of("path", "notes/agent.md"));
        assertThat(read.path("data").path("revision").asText()).isEqualTo(originalRevision);

        JsonNode updated = callTool("put_document", putArguments("notes/agent.md", "# Updated draft\n", originalRevision));
        String currentRevision = updated.path("data").path("revision").asText();
        assertThat(currentRevision).isNotEqualTo(originalRevision);

        JsonNode stale = callToolResult("put_document", putArguments("notes/agent.md", "# Stale overwrite\n", originalRevision));
        assertThat(stale.path("isError").asBoolean()).isTrue();
        assertError(stale, "REVISION_CONFLICT");

        HttpResponse<String> restRead = sendHttp("GET", "/api/v1/vault/documents?path=notes%2Fagent.md", null, null);
        assertThat(restRead.statusCode()).isEqualTo(200);
        JsonNode persisted = objectMapper.readTree(restRead.body());
        assertThat(persisted.path("data").path("content").asText()).contains("# Updated draft");
        assertThat(persisted.path("data").path("content").asText()).doesNotContain("# Stale overwrite");

        JsonNode nestedTree = callTool("get_vault_tree", Map.of());
        assertThat(nestedTree.path("data").get(0).path("children").get(0).path("path").asText())
                .isEqualTo("notes/agent.md");

        JsonNode collision = callToolResult("create_folder", Map.of("path", "notes"));
        assertThat(collision.path("isError").asBoolean()).isTrue();
        assertError(collision, "PATH_EXISTS");

        JsonNode missingParent = callToolResult("create_folder", Map.of("path", "absent/child"));
        assertThat(missingParent.path("isError").asBoolean()).isTrue();
        assertError(missingParent, "NOT_FOUND");
    }

    @Test
    void toolErrorsAreTextOnlyAndSuccessTextMatchesStructuredContent() throws Exception {
        JsonNode missing = callToolResult("get_document", Map.of("path", "missing.md"));
        assertThat(missing.path("isError").asBoolean()).isTrue();
        assertError(missing, "NOT_FOUND");

        JsonNode invalidPath = callToolResult("get_document", Map.of("path", "../outside.md"));
        assertError(invalidPath, "INVALID_PATH");

        callTool("get_vault_tree", Map.of());
    }

    @Test
    void uploadMoveAndDisabledDeleteKeepVaultRules() throws Exception {
        callTool("create_folder", Map.of("path", "mcp003"));
        JsonNode upload = callTool("upload_documents", Map.of("folder", "mcp003",
                "files", List.of(Map.of("name", "first.md", "content", "# Initial"))));
        assertThat(upload.path("success").asBoolean()).isTrue();
        assertThat(upload.path("data").get(0).path("status").asText()).isEqualTo("created");
        String initialRevision = upload.path("data").get(0).path("revision").asText();

        JsonNode replaced = callTool("upload_documents", Map.of("folder", "mcp003",
                "files", List.of(Map.of("name", "first.md", "content", "# Replaced",
                        "expectedRevision", initialRevision))));
        assertThat(replaced.path("data").get(0).path("status").asText()).isEqualTo("replaced");
        String replacementRevision = replaced.path("data").get(0).path("revision").asText();

        JsonNode duplicate = callToolResult("upload_documents", Map.of("folder", "mcp003", "files", List.of(
                Map.of("name", "duplicate.md", "content", "# One"),
                Map.of("name", "duplicate.md", "content", "# Two"))));
        assertError(duplicate, "DUPLICATE_BATCH_PATH");
        assertError(callToolResult("get_document", Map.of("path", "mcp003/duplicate.md")), "NOT_FOUND");

        JsonNode moveConflict = callToolResult("move_item", Map.of("from", "mcp003/first.md",
                "to", "mcp003/stale.md", "expectedRevision", initialRevision));
        assertError(moveConflict, "REVISION_CONFLICT");
        assertThat(callTool("get_document", Map.of("path", "mcp003/first.md")).path("data").path("revision").asText())
                .isEqualTo(replacementRevision);

        JsonNode createdNestedFolder = callTool("create_folder", Map.of("path", "mcp003/parent"));
        String staleFolderRevision = createdNestedFolder.path("data").path("revision").asText();
        callTool("upload_documents", Map.of("folder", "mcp003/parent",
                "files", List.of(Map.of("name", "child.md", "content", "# Child"))));
        JsonNode staleFolderMove = callToolResult("move_item", Map.of("from", "mcp003/parent",
                "to", "mcp003/parent-moved", "expectedRevision", staleFolderRevision));
        assertError(staleFolderMove, "REVISION_CONFLICT");

        JsonNode occupied = callTool("upload_documents", Map.of("folder", "mcp003",
                "files", List.of(Map.of("name", "occupied.md", "content", "# Occupied"))));
        JsonNode moveCollision = callToolResult("move_item", Map.of("from", "mcp003/first.md",
                "to", "mcp003/occupied.md", "expectedRevision", replacementRevision));
        assertError(moveCollision, "PATH_EXISTS");
        assertThat(callTool("get_document", Map.of("path", "mcp003/occupied.md")).path("data").path("revision").asText())
                .isEqualTo(occupied.path("data").get(0).path("revision").asText());

        JsonNode moved = callTool("move_item", Map.of("from", "mcp003/first.md",
                "to", "mcp003/renamed.md", "expectedRevision", replacementRevision));
        assertThat(moved.path("data").path("path").asText()).isEqualTo("mcp003/renamed.md");
        JsonNode currentTree = callTool("get_vault_tree", Map.of());
        String folderRevision = findNode(currentTree.path("data"), "mcp003").path("revision").asText();
        assertError(callToolResult("delete_item", Map.of("path", "mcp003",
                "expectedRevision", folderRevision)), "DELETE_DISABLED");
        JsonNode stillPresent = callTool("get_vault_tree", Map.of());
        assertThat(stillPresent.toString()).contains("mcp003/renamed.md");
    }

    private JsonNode callTool(String name, Map<String, ?> arguments) throws Exception {
        JsonNode response = rpcResult(sendRpc("tools/call", 3,
                Map.of("name", name, "arguments", arguments), sessionId));
        assertThat(response.path("isError").asBoolean()).as(response.toPrettyString()).isFalse();
        JsonNode structured = response.path("structuredContent");
        assertThat(structured.isObject()).isTrue();
        assertTextMatchesStructured(response);
        return structured;
    }

    private JsonNode callToolWithNullRevision() throws Exception {
        String request = """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"put_document","arguments":{"path":"notes/agent.md","content":"# First draft\\n","expectedRevision":null}}}
                """;
        JsonNode response = rpcResult(sendHttp("POST", "/mcp", request, sessionId));
        assertThat(response.path("isError").asBoolean()).as(response.toPrettyString()).isFalse();
        assertTextMatchesStructured(response);
        return response.path("structuredContent");
    }

    private JsonNode callToolResult(String name, Map<String, ?> arguments) throws Exception {
        return rpcResult(sendRpc("tools/call", 4,
                Map.of("name", name, "arguments", arguments), sessionId));
    }

    private void assertError(JsonNode result, String code) throws Exception {
        assertThat(result.path("isError").asBoolean()).isTrue();
        assertThat(result.has("structuredContent")).isFalse();
        JsonNode error = objectMapper.readTree(result.path("content").get(0).path("text").asText());
        assertThat(error.path("code").asText()).isEqualTo(code);
        assertThat(error.path("details").isArray()).isTrue();
    }

    private JsonNode findNode(JsonNode nodes, String path) {
        for (JsonNode node : nodes) {
            if (path.equals(node.path("path").asText())) return node;
            JsonNode found = findNode(node.path("children"), path);
            if (!found.isMissingNode()) return found;
        }
        return objectMapper.missingNode();
    }

    private Map<String, Object> putArguments(String path, String content, String expectedRevision) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("content", content);
        arguments.put("expectedRevision", expectedRevision);
        return arguments;
    }

    private HttpResponse<String> sendRpc(String method, int id, Map<String, ?> params, String session)
            throws Exception {
        return sendHttp("POST", "/mcp", objectMapper.writeValueAsString(
                Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params)), session);
    }

    private HttpResponse<String> sendHttp(String method, String path, String body, String session) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer local-test-token");
        if (session != null) {
            request.header("Mcp-Session-Id", session).header("MCP-Protocol-Version", "2025-11-25");
        }
        if (body == null) {
            request.GET();
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode rpcResult(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        int dataStart = body.indexOf("data:");
        if (dataStart >= 0) {
            body = body.substring(dataStart + "data:".length()).strip();
        }
        return objectMapper.readTree(body).path("result");
    }

    private void assertTextMatchesStructured(JsonNode toolResult) throws Exception {
        JsonNode textPayload = objectMapper.readTree(toolResult.path("content").get(0).path("text").asText());
        assertThat(textPayload).isEqualTo(toolResult.path("structuredContent"));
    }
}
