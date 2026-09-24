package io.github.nikhil8bph.markcraft.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"markcraft.mcp.enabled=true", "markcraft.mcp.token=local-test-token"})
class McpAccessIntegrationTests {

    @TempDir
    static Path temporaryDirectory;

    @DynamicPropertySource
    static void vaultRoot(DynamicPropertyRegistry registry) {
        registry.add("markcraft.vault.root", () -> temporaryDirectory.resolve("vault").toString());
    }

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void allMcpMethodsRequireTheOwnerToken() throws Exception {
        assertThat(send("GET", "/mcp", null, null, null).statusCode()).isEqualTo(401);
        assertThat(send("HEAD", "/mcp", null, null, null).statusCode()).isEqualTo(401);
        assertThat(send("POST", "/mcp", "{}", null, null).statusCode()).isEqualTo(401);
        assertThat(send("DELETE", "/mcp", null, null, null).statusCode()).isEqualTo(401);
        assertThat(send("OPTIONS", "/mcp", null, null, null).statusCode()).isEqualTo(401);
        assertThat(send("POST", "/mcp", "{}", "Bearer wrong", null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/mcp", null, "Bearer local-test-token", "http://evil.example")
                .statusCode()).isEqualTo(400);
        assertThat(send("POST", "/mcp", "{}", "Bearer local-test-token", "http://evil.example")
                .statusCode()).isEqualTo(400);
        assertThat(send("GET", "/mcp/other", null, "Bearer local-test-token", null)
                .statusCode()).isEqualTo(404);
        try (var socket = new Socket("127.0.0.1", port);
                var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.getOutputStream().write(("GET /mcp HTTP/1.1\r\nHost: evil.example\r\n"
                    + "Authorization: Bearer local-test-token\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            assertThat(reader.readLine()).contains("400");
        }
        assertThat(send("GET", "/api/v1/vault/tree", null, null, null).statusCode()).isEqualTo(200);
    }

    @Test
    void authorizedClientCanInitializeAndDiscoverToolCapability() throws Exception {
        String initialize = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
                  "protocolVersion":"2025-11-25","capabilities":{},
                  "clientInfo":{"name":"markcraft-test","version":"1.0"}}}
                """;
        var response = send("POST", "/mcp", initialize, "Bearer local-test-token", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("serverInfo", "markcraft", "tools");
        assertThat(response.body()).doesNotContain("resources", "prompts", "completions");

        var list = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer local-test-token")
                .header("MCP-Protocol-Version", "2025-11-25");
        response.headers().firstValue("Mcp-Session-Id").ifPresent(session -> list.header("Mcp-Session-Id", session));
        var discovered = client.send(list.POST(HttpRequest.BodyPublishers.ofString(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(discovered.statusCode()).isEqualTo(200);
        assertThat(discovered.body()).contains("\"tools\"");
    }

    private HttpResponse<String> send(String method, String path, String body, String authorization, String origin)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Accept", "application/json, text/event-stream");
        if (authorization != null) builder.header("Authorization", authorization);
        if (origin != null) builder.header("Origin", origin);
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
