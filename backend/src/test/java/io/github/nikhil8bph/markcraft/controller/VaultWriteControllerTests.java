package io.github.nikhil8bph.markcraft.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VaultWriteControllerTests {

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

    private Path root() {
        return temporaryDirectory.resolve("vault");
    }

    @BeforeEach
    void clearVault() throws Exception {
        try (var entries = Files.walk(root())) {
            for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(root())) Files.delete(path);
            }
        }
    }

    @Test
    void createsNormalizedDocumentAndReturnsExactlyPersistedBytesAndRevision() throws Exception {
        Files.createDirectory(root().resolve("Notes"));
        var created = put("Notes/idea.md", "# Café 📄\n", null, "*", null);
        JsonNode data = json.readTree(created.body()).get("data");
        String disk = Files.readString(root().resolve("Notes/idea.md"));
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(data.get("content").asText()).isEqualTo(disk);
        assertThat(disk).startsWith("---\ntype: concept\ntitle: \"Café 📄\"\n---\n\n# Café 📄\n");
        assertThat(data.get("revision").asText()).isEqualTo(sha256(disk));
        assertThat(data.get("size").asLong()).isEqualTo(Files.size(root().resolve("Notes/idea.md")));
        assertThat(created.headers().firstValue("ETag")).contains('"' + sha256(disk) + '"');
        assertThat(json.readTree(get("Notes/idea.md").body()).get("data").get("content").asText()).isEqualTo(disk);
    }

    @Test
    void updatesOnlyCurrentRevisionAndPreservesValidUnknownYaml() throws Exception {
        String initial = "---\ntype: guide\ncustom: [one, two]\n---\n# Initial\n";
        Files.writeString(root().resolve("guide.md"), initial);
        String etag = get("guide.md").headers().firstValue("ETag").orElseThrow();
        String replacement = "---\ntype: guide\ncustom: [one, two]\n---\n# Changed\n";

        var updated = put("guide.md", replacement, etag, null, null);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(Files.readString(root().resolve("guide.md"))).isEqualTo(replacement);
        assertThat(json.readTree(updated.body()).get("data").get("revision").asText())
                .isEqualTo(sha256(replacement));
        assertThat(updated.headers().firstValue("ETag")).contains('"' + sha256(replacement) + '"');

        var stale = put("guide.md", "# Lost", etag, null, null);
        assertError(stale, 412, "REVISION_CONFLICT");
        assertThat(Files.readString(root().resolve("guide.md"))).isEqualTo(replacement);
    }

    @Test
    void createCollisionAndInvalidConditionsCannotOverwrite() throws Exception {
        Files.writeString(root().resolve("idea.md"), "original");
        assertError(put("idea.md", "replacement", null, "*", null), 412, "PATH_EXISTS");
        assertError(put("idea.md", "replacement", null, null, null), 400, "PRECONDITION_REQUIRED");
        assertError(put("idea.md", "replacement", "\"bad\"", null, null), 400, "INVALID_REQUEST");
        assertError(put("idea.md", "replacement", "\"bad\"", "*", null), 400, "INVALID_REQUEST");
        assertError(put("idea.md", "replacement", null, "wrong", null), 400, "INVALID_REQUEST");
        assertThat(Files.readString(root().resolve("idea.md"))).isEqualTo("original");
    }

    @Test
    void missingTargetAndParentAreNotCreatedByUpdateOrCreate() throws Exception {
        assertError(put("missing.md", "# Missing", '"' + "0".repeat(64) + '"', null, null), 404, "NOT_FOUND");
        assertError(put("Missing/file.md", "# Missing", null, "*", null), 404, "NOT_FOUND");
        assertThat(Files.exists(root().resolve("Missing"))).isFalse();
        assertThat(Files.exists(root().resolve("missing.md"))).isFalse();
    }

    @Test
    void malformedMetadataAndInvalidBodyCannotReplaceExistingFile() throws Exception {
        Files.writeString(root().resolve("idea.md"), "original");
        String etag = get("idea.md").headers().firstValue("ETag").orElseThrow();
        for (String content : new String[] {"---\ntype: guide\nNo close", "---\n- invalid\n---\n"}) {
            assertError(put("idea.md", content, etag, null, null), 422, "INVALID_FRONTMATTER");
        }
        assertError(rawPut("idea.md", "{}", etag, null, null), 400, "INVALID_REQUEST");
        assertError(rawPut("idea.md", "{\"content\":\"# Valid\",\"extra\":true}", etag, null, null),
                400, "INVALID_REQUEST");
        assertThat(Files.readString(root().resolve("idea.md"))).isEqualTo("original");
    }

    @Test
    void rejectsPathEscapeAndCrossOriginMutation() throws Exception {
        Path outside = temporaryDirectory.resolve("outside");
        Files.createDirectories(outside);
        Files.createSymbolicLink(root().resolve("linked"), outside);
        assertError(put("../outside.md", "# Escape", null, "*", null), 400, "INVALID_PATH");
        assertError(put("linked/outside.md", "# Escape", null, "*", null), 400, "INVALID_PATH");
        assertError(put("safe.md", "# Safe", null, "*", "http://evil.example"), 400, "INVALID_REQUEST");
        assertThat(Files.exists(outside.resolve("outside.md"))).isFalse();
        assertThat(Files.exists(root().resolve("safe.md"))).isFalse();
    }

    private HttpResponse<String> put(String path, String content, String ifMatch, String ifNoneMatch, String origin)
            throws Exception {
        return rawPut(path, json.writeValueAsString(Map.of("content", content)), ifMatch, ifNoneMatch, origin);
    }

    private HttpResponse<String> rawPut(String path, String body, String ifMatch, String ifNoneMatch, String origin)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (ifMatch != null) request.header("If-Match", ifMatch);
        if (ifNoneMatch != null) request.header("If-None-Match", ifNoneMatch);
        if (origin != null) request.header("Origin", origin);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + "/api/v1/vault/documents?path=" 
                + URLEncoder.encode(path, StandardCharsets.UTF_8));
    }

    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo(code);
        assertThat(body.get("error").get("details")).isEmpty();
        assertThat(response.body()).doesNotContain(root().toString());
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
