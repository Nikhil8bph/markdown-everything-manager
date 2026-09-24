package io.github.nikhil8bph.markcraft.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikhil8bph.markcraft.repo.AtomicFileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"markcraft.mcp.enabled=true", "markcraft.mcp.token=local-test-token"})
@Import(McpUploadBatchIntegrationTests.FailingWriterConfiguration.class)
class McpUploadBatchIntegrationTests {

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
                "clientInfo", Map.of("name", "markcraft-batch-test", "version", "1.0")), null);
        assertThat(response.statusCode()).isEqualTo(200);
        sessionId = response.headers().firstValue("Mcp-Session-Id").orElse(null);
    }

    @Test
    void oversizedBatchFailsPreflightWithoutWritingAnyFile() throws Exception {
        List<Map<String, String>> files = List.of(
                Map.of("name", "would-have-been-first.md", "content", "# Valid"),
                Map.of("name", "too-large.md", "content", "x".repeat(25_000_001)));
        JsonNode result = callToolResult("upload_documents", Map.of("folder", "", "files", files));
        assertThat(result.path("isError").asBoolean()).isTrue();
        assertThat(result.has("structuredContent")).isFalse();
        JsonNode error = objectMapper.readTree(result.path("content").get(0).path("text").asText());
        assertThat(error.path("code").asText()).isEqualTo("PAYLOAD_TOO_LARGE");
        assertThat(Files.exists(temporaryDirectory.resolve("vault/would-have-been-first.md"))).isFalse();
        assertThat(Files.exists(temporaryDirectory.resolve("vault/too-large.md"))).isFalse();
    }

    @Test
    void partialWriteReportsEachOutcomeAndDoesNotReplayLaterFiles() throws Exception {
        List<Map<String, String>> files = List.of(
                Map.of("name", "committed.md", "content", "# Committed"),
                Map.of("name", "failed.md", "content", "# Failed"),
                Map.of("name", "not-attempted.md", "content", "# Not attempted"));
        JsonNode result = callToolResult("upload_documents", Map.of("folder", "", "files", files));

        assertThat(result.path("isError").asBoolean()).isFalse();
        JsonNode output = result.path("structuredContent");
        assertThat(output.path("success").asBoolean()).isFalse();
        assertThat(output.path("data").get(0).path("status").asText()).isEqualTo("created");
        assertThat(output.path("data").get(1).path("status").asText()).isEqualTo("failed");
        assertThat(output.path("data").get(1).path("error").path("code").asText()).isEqualTo("STORAGE_FAILURE");
        assertThat(output.path("data").get(2).path("status").asText()).isEqualTo("notAttempted");
        JsonNode textPayload = objectMapper.readTree(result.path("content").get(0).path("text").asText());
        assertThat(textPayload).isEqualTo(output);
        assertThat(Files.exists(temporaryDirectory.resolve("vault/committed.md"))).isTrue();
        assertThat(Files.exists(temporaryDirectory.resolve("vault/failed.md"))).isFalse();
        assertThat(Files.exists(temporaryDirectory.resolve("vault/not-attempted.md"))).isFalse();
    }

    private JsonNode callToolResult(String name, Map<String, ?> arguments) throws Exception {
        return rpcResult(sendRpc("tools/call", 3,
                Map.of("name", name, "arguments", arguments), sessionId));
    }

    private HttpResponse<String> sendRpc(String method, int id, Map<String, ?> params, String session)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer local-test-token")
                .header("Content-Type", "application/json");
        if (session != null) {
            request.header("Mcp-Session-Id", session).header("MCP-Protocol-Version", "2025-11-25");
        }
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params)))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode rpcResult(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        int dataStart = body.indexOf("data:");
        if (dataStart >= 0) body = body.substring(dataStart + "data:".length()).strip();
        return objectMapper.readTree(body).path("result");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingWriterConfiguration {

        @Bean
        @Primary
        AtomicFileWriter failingWriter() {
            return new AtomicFileWriter() {
                private final AtomicInteger commits = new AtomicInteger();

                @Override
                protected void commit(Path temporary, Path destination, boolean create) throws IOException {
                    if (commits.incrementAndGet() == 2) {
                        throw new IOException("induced second-upload failure");
                    }
                    super.commit(temporary, destination, create);
                }
            };
        }
    }
}
