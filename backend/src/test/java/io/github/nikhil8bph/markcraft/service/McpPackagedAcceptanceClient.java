package io.github.nikhil8bph.markcraft.service;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs real MCP client calls against a separately launched packaged MarkCraft jar. */
public final class McpPackagedAcceptanceClient {

    private McpPackagedAcceptanceClient() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Usage: McpPackagedAcceptanceClient <mcp-url> <disabled|enabled>");
        }
        String token = System.getenv("MARKCRAFT_MCP_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("MARKCRAFT_MCP_TOKEN must be set in the client environment");
        }
        var transport = HttpClientStreamableHttpTransport.builder(arguments[0])
                .endpoint("/mcp")
                .httpRequestCustomizer((request, method, uri, body, context) ->
                        request.header("Authorization", "Bearer " + token))
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try (McpSyncClient client = McpClient.sync(transport)
                .clientInfo(Implementation.builder("markcraft-packaged-acceptance", "1.0").build())
                .requestTimeout(Duration.ofSeconds(15))
                .build()) {
            client.initialize();
            verifyDiscovery(client);
            if ("disabled".equals(arguments[1])) {
                verifyDeletionDisabled(client);
            } else if ("enabled".equals(arguments[1])) {
                verifyFullWorkflow(client);
            } else {
                throw new IllegalArgumentException("Mode must be disabled or enabled");
            }
        }
    }

    private static void verifyDiscovery(McpSyncClient client) {
        ListToolsResult discovered = client.listTools();
        List<String> names = discovered.tools().stream().map(tool -> tool.name()).sorted().toList();
        List<String> expected = List.of("create_folder", "delete_item", "get_document", "get_vault_tree",
                "move_item", "put_document", "upload_documents");
        require(names.equals(expected), "Expected all seven catalog tools, found " + names);
        require(discovered.tools().stream().allMatch(tool -> tool.inputSchema().containsKey("$defs")
                && tool.outputSchema().containsKey("$defs")), "Tool schemas were not self-contained");
        System.out.println("PASS: SDK client initialized and discovered seven catalog tools with resolved schemas");
    }

    private static void verifyDeletionDisabled(McpSyncClient client) {
        callSuccess(client, "create_folder", Map.of("path", "client-disabled"));
        Map<String, Object> create = new HashMap<>();
        create.put("path", "client-disabled/sentinel.md");
        create.put("content", "# Must remain\n");
        create.put("expectedRevision", null);
        callSuccess(client, "put_document", create);

        Map<?, ?> tree = object(callSuccess(client, "get_vault_tree", Map.of()).structuredContent());
        Map<?, ?> folder = findNode(list(tree.get("data")), "client-disabled");
        CallToolResult denied = call(client, "delete_item", Map.of("path", "client-disabled",
                "expectedRevision", folder.get("revision")));
        assertError(denied, "DELETE_DISABLED");
        callSuccess(client, "get_document", Map.of("path", "client-disabled/sentinel.md"));
        System.out.println("PASS: SDK client confirmed DELETE_DISABLED and retained the vault item");
    }

    private static void verifyFullWorkflow(McpSyncClient client) {
        Map<?, ?> initialTree = object(callSuccess(client, "get_vault_tree", Map.of()).structuredContent());
        require(list(initialTree.get("data")).isEmpty(), "Packaged acceptance vault was not empty at startup");

        callSuccess(client, "create_folder", Map.of("path", "mcp-acceptance"));
        Map<String, Object> createDocument = new HashMap<>();
        createDocument.put("path", "mcp-acceptance/agent.md");
        createDocument.put("content", "# Created by packaged MCP client\n");
        createDocument.put("expectedRevision", null);
        Map<?, ?> created = data(callSuccess(client, "put_document", createDocument));
        String firstRevision = string(created.get("revision"));
        Map<?, ?> read = data(callSuccess(client, "get_document", Map.of("path", "mcp-acceptance/agent.md")));
        require(firstRevision.equals(read.get("revision")), "Read did not return the created revision");

        CallToolResult stale = call(client, "put_document", putDocument("mcp-acceptance/agent.md",
                "# Stale attempt\n", "0".repeat(64)));
        assertError(stale, "REVISION_CONFLICT");
        Map<?, ?> replaced = data(callSuccess(client, "put_document", putDocument(
                "mcp-acceptance/agent.md", "# Updated by packaged MCP client\n", firstRevision)));
        String currentRevision = string(replaced.get("revision"));
        require(!firstRevision.equals(currentRevision), "Successful update did not advance the revision");

        Map<String, Object> uploadFile = new HashMap<>();
        uploadFile.put("name", "uploaded.md");
        uploadFile.put("content", "# Uploaded through MCP\n");
        uploadFile.put("expectedRevision", null);
        Map<?, ?> uploadCreated = upload(client, "mcp-acceptance", List.of(uploadFile));
        Map<?, ?> uploadResult = object(list(uploadCreated.get("data")).get(0));
        require("created".equals(uploadResult.get("status")), "Upload did not create the file");

        Map<String, Object> replacementFile = new HashMap<>();
        replacementFile.put("name", "uploaded.md");
        replacementFile.put("content", "# Replaced through MCP\n");
        replacementFile.put("expectedRevision", uploadResult.get("revision"));
        Map<?, ?> uploadReplaced = upload(client, "mcp-acceptance", List.of(replacementFile));
        Map<?, ?> replacementResult = object(list(uploadReplaced.get("data")).get(0));
        require("replaced".equals(replacementResult.get("status")), "Upload did not replace the selected revision");

        Map<?, ?> moved = data(callSuccess(client, "move_item", Map.of(
                "from", "mcp-acceptance/uploaded.md", "to", "mcp-acceptance/moved.md",
                "expectedRevision", replacementResult.get("revision"))));
        require("mcp-acceptance/moved.md".equals(moved.get("path")), "Move returned the wrong destination");
        Map<?, ?> deletedFile = data(callSuccess(client, "delete_item", Map.of(
                "path", "mcp-acceptance/moved.md", "expectedRevision", moved.get("revision"))));
        require(Boolean.TRUE.equals(deletedFile.get("deleted")), "Current-revision file delete failed");

        callSuccess(client, "create_folder", Map.of("path", "mcp-acceptance/nested"));
        Map<String, Object> nestedUpload = new HashMap<>();
        nestedUpload.put("name", "child.md");
        nestedUpload.put("content", "# Recursive delete check\n");
        nestedUpload.put("expectedRevision", null);
        upload(client, "mcp-acceptance/nested", List.of(nestedUpload));
        Map<?, ?> currentTree = object(callSuccess(client, "get_vault_tree", Map.of()).structuredContent());
        Map<?, ?> acceptanceFolder = findNode(list(currentTree.get("data")), "mcp-acceptance");
        Map<?, ?> deletedFolder = data(callSuccess(client, "delete_item", Map.of(
                "path", "mcp-acceptance", "expectedRevision", acceptanceFolder.get("revision"))));
        require(Boolean.TRUE.equals(deletedFolder.get("deleted")), "Recursive folder delete failed");
        Map<?, ?> afterDelete = object(callSuccess(client, "get_vault_tree", Map.of()).structuredContent());
        require(findOptionalNode(list(afterDelete.get("data")), "mcp-acceptance") == null,
                "Recursive delete left the folder in the vault");

        Map<String, Object> visibleDocument = new HashMap<>();
        visibleDocument.put("path", "MCP-agent-visible.md");
        visibleDocument.put("content", "# Created by an MCP agent\n\nVisible through the MarkCraft workspace.\n");
        visibleDocument.put("expectedRevision", null);
        callSuccess(client, "put_document", visibleDocument);
        System.out.println("PASS: create/read/update, stale conflict, upload/create/replace, move, file delete, recursive folder delete");
        System.out.println("PASS: left MCP-agent-visible.md in the packaged vault for browser verification");
    }

    private static Map<?, ?> upload(McpSyncClient client, String folder, List<Map<String, Object>> files) {
        return object(callSuccess(client, "upload_documents", Map.of("folder", folder, "files", files)).structuredContent());
    }

    private static Map<String, Object> putDocument(String path, String content, String revision) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("content", content);
        arguments.put("expectedRevision", revision);
        return arguments;
    }

    private static CallToolResult callSuccess(McpSyncClient client, String name, Map<String, Object> arguments) {
        CallToolResult result = call(client, name, arguments);
        require(!Boolean.TRUE.equals(result.isError()), name + " failed: " + text(result));
        require(result.structuredContent() != null, name + " returned no structured success payload");
        require(!result.content().isEmpty(), name + " returned no text fallback");
        return result;
    }

    private static CallToolResult call(McpSyncClient client, String name, Map<String, Object> arguments) {
        return client.callTool(new CallToolRequest(name, arguments));
    }

    private static void assertError(CallToolResult result, String code) {
        require(Boolean.TRUE.equals(result.isError()), "Expected error " + code + " but got " + text(result));
        require(result.structuredContent() == null, "Failed MCP calls must not include structuredContent");
        require(text(result).contains("\"code\":\"" + code + "\""), "Expected " + code + " in: " + text(result));
    }

    private static String text(CallToolResult result) {
        if (result.content().isEmpty() || !(result.content().get(0) instanceof TextContent content)) return "";
        return content.text();
    }

    private static Map<?, ?> data(CallToolResult result) {
        return object(object(result.structuredContent()).get("data"));
    }

    private static Map<?, ?> object(Object value) {
        require(value instanceof Map<?, ?>, "Expected an object, got " + value);
        return (Map<?, ?>) value;
    }

    private static List<?> list(Object value) {
        require(value instanceof List<?>, "Expected an array, got " + value);
        return (List<?>) value;
    }

    private static String string(Object value) {
        require(value instanceof String, "Expected a string, got " + value);
        return (String) value;
    }

    private static Map<?, ?> findNode(List<?> nodes, String path) {
        Map<?, ?> node = findOptionalNode(nodes, path);
        require(node != null, "Could not find vault node " + path);
        return node;
    }

    private static Map<?, ?> findOptionalNode(List<?> nodes, String path) {
        for (Object candidate : nodes) {
            Map<?, ?> node = object(candidate);
            if (path.equals(node.get("path"))) return node;
            Object children = node.get("children");
            if (children instanceof List<?> childList) {
                Map<?, ?> found = findOptionalNode(childList, path);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
