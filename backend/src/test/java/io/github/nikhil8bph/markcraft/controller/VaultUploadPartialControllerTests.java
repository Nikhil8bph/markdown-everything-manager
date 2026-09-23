package io.github.nikhil8bph.markcraft.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.request.UploadRequest;
import io.github.nikhil8bph.markcraft.repo.AtomicFileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VaultUploadPartialControllerTests.FailingWriterConfiguration.class)
class VaultUploadPartialControllerTests {

    @TempDir
    static Path temporaryDirectory;

    @DynamicPropertySource
    static void configureVault(DynamicPropertyRegistry registry) {
        registry.add("markcraft.vault.root", () -> temporaryDirectory.resolve("vault").toString());
    }

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void reportsCommittedFailedAndNotAttemptedFilesAfterSecondCommitFails() throws Exception {
        UploadRequest batch = new UploadRequest("", List.of(
                new UploadFileRequest("one.md", "# One", null),
                new UploadFileRequest("two.md", "# Two", null),
                new UploadFileRequest("three.md", "# Three", null)));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/vault/uploads"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(batch)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode body = json.readTree(response.body());
        JsonNode results = body.get("data");
        assertThat(response.statusCode()).isEqualTo(207);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(results).hasSize(3);
        assertThat(results.get(0).get("status").asText()).isEqualTo("created");
        String persisted = Files.readString(temporaryDirectory.resolve("vault/one.md"));
        assertThat(results.get(0).get("revision").asText()).isEqualTo(sha256(persisted));
        assertThat(results.get(1).get("status").asText()).isEqualTo("failed");
        assertThat(results.get(1).get("error").get("code").asText()).isEqualTo("STORAGE_FAILURE");
        assertThat(results.get(1).has("revision")).isFalse();
        assertThat(results.get(2).get("status").asText()).isEqualTo("notAttempted");
        assertThat(results.get(2).has("revision")).isFalse();
        assertThat(Files.exists(temporaryDirectory.resolve("vault/two.md"))).isFalse();
        assertThat(Files.exists(temporaryDirectory.resolve("vault/three.md"))).isFalse();
        try (var entries = Files.list(temporaryDirectory.resolve("vault"))) {
            assertThat(entries.map(path -> path.getFileName().toString())).containsExactly("one.md");
        }
        assertThat(response.body()).doesNotContain(temporaryDirectory.toString());
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingWriterConfiguration {

        @Bean
        @Primary
        AtomicFileWriter failingWriter() {
            return new AtomicFileWriter() {
                private final AtomicInteger committed = new AtomicInteger();

                @Override
                protected void commit(Path temporary, Path destination, boolean create) throws IOException {
                    if (committed.incrementAndGet() == 2) {
                        throw new IOException("induced second-upload failure");
                    }
                    super.commit(temporary, destination, create);
                }
            };
        }
    }
}
