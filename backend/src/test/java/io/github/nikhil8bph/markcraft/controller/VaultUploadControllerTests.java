package io.github.nikhil8bph.markcraft.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.request.UploadRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
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
class VaultUploadControllerTests {

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
    void createsBatchInExistingFolderWithRevisionsMatchingDisk() throws Exception {
        Files.createDirectory(root().resolve("Notes"));
        var response = upload("Notes", List.of(file("one.md", "# One", null),
                file("two.md", "---\ntype: guide\ncustom: keep\n---\nTwo", null)));
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("data")).hasSize(2);
        assertThat(body.get("data").get(0).get("status").asText()).isEqualTo("created");
        assertThat(body.get("data").get(0).get("path").asText()).isEqualTo("Notes/one.md");
        assertThat(body.get("data").get(1).get("status").asText()).isEqualTo("created");
        String first = Files.readString(root().resolve("Notes/one.md"));
        String second = Files.readString(root().resolve("Notes/two.md"));
        assertThat(first).startsWith("---\ntype: concept\ntitle: \"One\"\n---\n\n# One");
        assertThat(second).isEqualTo("---\ntype: guide\ncustom: keep\n---\nTwo");
        assertThat(body.get("data").get(0).get("revision").asText()).isEqualTo(sha256(first));
        assertThat(body.get("data").get(1).get("revision").asText()).isEqualTo(sha256(second));
    }

    @Test
    void confirmedRevisionReplacesExistingFileWithoutLosingOtherBatchCreates() throws Exception {
        Files.writeString(root().resolve("existing.md"), "---\ntype: note\n---\nOld");
        String revision = sha256(Files.readString(root().resolve("existing.md")));
        var response = upload("", List.of(file("new.md", "# New", null),
                file("existing.md", "# Changed", revision)));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = json.readTree(response.body()).get("data");
        assertThat(data.get(0).get("status").asText()).isEqualTo("created");
        assertThat(data.get(1).get("status").asText()).isEqualTo("replaced");
        assertThat(Files.readString(root().resolve("new.md"))).contains("# New");
        String changed = Files.readString(root().resolve("existing.md"));
        assertThat(changed).contains("# Changed").doesNotContain("Old");
        assertThat(data.get(1).get("revision").asText()).isEqualTo(sha256(changed));
    }

    @Test
    void duplicateOrOccupiedSecondPathRejectsWholeBatchBeforeWrites() throws Exception {
        assertError(upload("", List.of(file("first.md", "# First", null),
                file("first.md", "# Again", null))), 409, "DUPLICATE_BATCH_PATH");
        assertThat(Files.exists(root().resolve("first.md"))).isFalse();

        Files.writeString(root().resolve("occupied.md"), "original");
        assertError(upload("", List.of(file("first.md", "# First", null),
                file("occupied.md", "# Collision", null))), 412, "PATH_EXISTS");
        assertThat(Files.exists(root().resolve("first.md"))).isFalse();
        assertThat(Files.readString(root().resolve("occupied.md"))).isEqualTo("original");
    }

    @Test
    void staleOverwriteOrAbsentOverwriteRejectsWholeBatch() throws Exception {
        Files.writeString(root().resolve("existing.md"), "original");
        assertError(upload("", List.of(file("new.md", "# New", null),
                file("existing.md", "# Stale", "0".repeat(64)))), 412, "REVISION_CONFLICT");
        assertThat(Files.exists(root().resolve("new.md"))).isFalse();
        assertThat(Files.readString(root().resolve("existing.md"))).isEqualTo("original");

        assertError(upload("", List.of(file("new.md", "# New", null),
                file("missing.md", "# Missing", "0".repeat(64)))), 404, "NOT_FOUND");
        assertThat(Files.exists(root().resolve("new.md"))).isFalse();
    }

    @Test
    void malformedMetadataAndUnsupportedNamesRejectWholeBatch() throws Exception {
        assertError(upload("", List.of(file("valid.md", "# Valid", null),
                file("bad.md", "---\ntype: note\nmissing close", null))), 422, "INVALID_FRONTMATTER");
        assertThat(Files.exists(root().resolve("valid.md"))).isFalse();

        assertError(upload("", List.of(file("valid.md", "# Valid", null),
                file("bad.txt", "Text", null))), 422, "UNSUPPORTED_FILE");
        assertThat(Files.exists(root().resolve("valid.md"))).isFalse();
        assertError(upload("", List.of(file("../escape.md", "# Escape", null))), 400, "INVALID_PATH");
        assertThat(Files.exists(temporaryDirectory.resolve("escape.md"))).isFalse();
    }

    @Test
    void oversizedCombinedBatchIsRejectedBeforeAnyFileIsWritten() throws Exception {
        String first = "x".repeat(12_500_000);
        String second = "y".repeat(12_500_001);
        assertError(upload("", List.of(file("first.md", first, null), file("second.md", second, null))),
                413, "PAYLOAD_TOO_LARGE");
        assertThat(Files.exists(root().resolve("first.md"))).isFalse();
        assertThat(Files.exists(root().resolve("second.md"))).isFalse();
    }

    @Test
    void absentFolderAndInvalidRevisionDoNotWrite() throws Exception {
        assertError(upload("Missing", List.of(file("one.md", "# One", null))), 404, "NOT_FOUND");
        assertError(upload("", List.of(file("one.md", "# One", "bad"))), 400, "INVALID_REQUEST");
        assertThat(Files.exists(root().resolve("one.md"))).isFalse();
    }

    @Test
    void rejectsSymlinkFolderAndSymlinkFilenameWithoutFollowingTargets() throws Exception {
        Path outside = temporaryDirectory.resolve("outside");
        Files.createDirectories(outside);
        Files.writeString(outside.resolve("private.md"), "secret");
        Files.createSymbolicLink(root().resolve("linked"), outside);
        Files.createSymbolicLink(root().resolve("linked.md"), outside.resolve("private.md"));

        assertError(upload("linked", List.of(file("new.md", "# New", null))), 400, "INVALID_PATH");
        assertError(upload("", List.of(file("linked.md", "# New", null))), 400, "INVALID_PATH");
        assertThat(Files.readString(outside.resolve("private.md"))).isEqualTo("secret");
        assertThat(Files.exists(outside.resolve("new.md"))).isFalse();
    }

    @Test
    void missingExpectedRevisionFieldAndUnknownFieldsAreRejected() throws Exception {
        assertError(rawUpload("{\"folder\":\"\",\"files\":[{\"name\":\"one.md\",\"content\":\"# One\"}] }"),
                400, "INVALID_REQUEST");
        assertError(rawUpload("{\"folder\":\"\",\"files\":[{\"name\":\"one.md\",\"content\":\"# One\",\"expectedRevision\":null,\"extra\":true}]}"),
                400, "INVALID_REQUEST");
        assertError(rawUpload("{\"folder\":\"\",\"files\":[]}"), 400, "INVALID_REQUEST");
        assertError(rawUpload("{\"folder\":\"\",\"files\":[null]}"), 400, "INVALID_REQUEST");
        assertThat(Files.exists(root().resolve("one.md"))).isFalse();
    }

    private UploadFileRequest file(String name, String content, String revision) {
        return new UploadFileRequest(name, content, revision);
    }

    private HttpResponse<String> upload(String folder, List<UploadFileRequest> files) throws Exception {
        return rawUpload(json.writeValueAsString(new UploadRequest(folder, files)));
    }

    private HttpResponse<String> rawUpload(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/vault/uploads"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
