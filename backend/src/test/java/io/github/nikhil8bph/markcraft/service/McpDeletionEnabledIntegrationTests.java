package io.github.nikhil8bph.markcraft.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "markcraft.mcp.enabled=true", "markcraft.mcp.token=local-test-token",
        "markcraft.mcp.agent-deletion-enabled=true"})
class McpDeletionEnabledIntegrationTests {

    @TempDir
    static Path temporaryDirectory;

    @DynamicPropertySource
    static void vaultRoot(DynamicPropertyRegistry registry) {
        registry.add("markcraft.vault.root", () -> temporaryDirectory.resolve("vault").toString());
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newHttpClient();
    private String sessionId;

    @BeforeEach
    void initializeProtocolSession() throws Exception {
        HttpResponse<String> response = sendRpc("initialize", 1, Map.of(
                "protocolVersion", "2025-11-25", "capabilities", Map.of(),
                "clientInfo", Map.of("name", "markcraft-delete-test", "version", "1.0")), null);
        assertThat(response.statusCode()).isEqualTo(200);
        sessionId = response.headers().firstValue("Mcp-Session-Id").orElse(null);
        JsonNode listing = rpcResult(sendRpc("tools/list", 2, Map.of(), sessionId));
        List<String> names = new ArrayList<>();
        for (JsonNode tool : listing.path("tools")) names.add(tool.path("name").asText());
        assertThat(names).contains("delete_item");
    }

    @Test
    void enabledDeletionPermanentlyRemovesTheCurrentFolderAndDescendants() throws Exception {
        assertSuccess(callTool("create_folder", Map.of("path", "delete-me")));
        assertSuccess(callTool("create_folder", Map.of("path", "delete-me/nested")));
        assertSuccess(callTool("upload_documents", Map.of("folder", "delete-me/nested",
                "files", List.of(Map.of("name", "child.md", "content", "# Child")))));

        JsonNode tree = callTool("get_vault_tree", Map.of());
        String currentRevision = findNode(tree.path("data"), "delete-me").path("revision").asText();
        JsonNode deleted = callTool("delete_item", Map.of("path", "delete-me",
                "expectedRevision", currentRevision));

        assertThat(deleted.path("success").asBoolean()).isTrue();
        assertThat(deleted.path("data").path("path").asText()).isEqualTo("delete-me");
        assertThat(deleted.path("data").path("deleted").asBoolean()).isTrue();
        assertThat(callTool("get_vault_tree", Map.of()).path("data").toString()).doesNotContain("delete-me");
        JsonNode missing = callToolResult("get_document", Map.of("path", "delete-me/nested/child.md"));
        assertThat(missing.path("isError").asBoolean()).isTrue();
        assertThat(missing.has("structuredContent")).isFalse();
        JsonNode error = objectMapper.readTree(missing.path("content").get(0).path("text").asText());
        assertThat(error.path("code").asText()).isEqualTo("NOT_FOUND");
    }

    private JsonNode findNode(JsonNode nodes, String path) {
        for (JsonNode node : nodes) {
            if (path.equals(node.path("path").asText())) return node;
            JsonNode found = findNode(node.path("children"), path);
            if (!found.isMissingNode()) return found;
        }
        return objectMapper.missingNode();
    }

    private JsonNode callTool(String name, Map<String, ?> arguments) throws Exception {
        JsonNode result = rpcResult(sendRpc("tools/call", 3,
                Map.of("name", name, "arguments", arguments), sessionId));
        assertThat(result.path("isError").asBoolean()).isFalse();
        assertThat(result.path("structuredContent").isObject()).isTrue();
        JsonNode textPayload = objectMapper.readTree(result.path("content").get(0).path("text").asText());
        assertThat(textPayload).isEqualTo(result.path("structuredContent"));
        return result.path("structuredContent");
    }

    private void assertSuccess(JsonNode result) {
        assertThat(result.path("success").asBoolean()).isTrue();
    }

    private JsonNode callToolResult(String name, Map<String, ?> arguments) throws Exception {
        return rpcResult(sendRpc("tools/call", 4,
                Map.of("name", name, "arguments", arguments), sessionId));
    }

    private HttpResponse<String> sendRpc(String method, int id, Map<String, ?> params, String session)
            throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer local-test-token")
                .header("Content-Type", "application/json");
        if (session != null) {
            request.header("Mcp-Session-Id", session).header("MCP-Protocol-Version", "2025-11-25");
        }
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode rpcResult(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        int dataStart = body.indexOf("data:");
        if (dataStart >= 0) body = body.substring(dataStart + "data:".length()).strip();
        return objectMapper.readTree(body).path("result");
    }
}
