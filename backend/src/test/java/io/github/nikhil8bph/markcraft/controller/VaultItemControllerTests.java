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
class VaultItemControllerTests {

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
    void createsEmptyFoldersWithoutImplicitParentsAndRejectsCollisions() throws Exception {
        var created = folder("Notes");
        JsonNode node = data(created);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(node.get("path").asText()).isEqualTo("Notes");
        assertThat(node.get("type").asText()).isEqualTo("folder");
        assertThat(node.get("children")).isEmpty();
        assertThat(node.has("size")).isFalse();
        assertThat(node.get("revision").asText()).isEqualTo(sha256(""));
        assertThat(Files.isDirectory(root().resolve("Notes"))).isTrue();

        assertThat(folder("Notes/Child").statusCode()).isEqualTo(201);
        assertError(folder("Notes"), 409, "PATH_EXISTS");
        assertError(folder("Missing/Child"), 404, "NOT_FOUND");
        Files.writeString(root().resolve("occupied"), "file");
        assertError(folder("occupied"), 409, "PATH_EXISTS");
        assertThat(Files.exists(root().resolve("Missing"))).isFalse();
    }

    @Test
    void movesFileWithoutOverwriteAndReturnsNewNode() throws Exception {
        Files.createDirectory(root().resolve("Notes"));
        Files.writeString(root().resolve("Notes/idea.md"), "# Idea");
        String revision = fileRevision("Notes/idea.md");
        var moved = move("Notes/idea.md", "renamed.md", quote(revision));

        assertThat(moved.statusCode()).isEqualTo(200);
        assertThat(data(moved).get("path").asText()).isEqualTo("renamed.md");
        assertThat(data(moved).get("revision").asText()).isEqualTo(revision);
        assertThat(Files.exists(root().resolve("Notes/idea.md"))).isFalse();
        assertThat(Files.readString(root().resolve("renamed.md"))).isEqualTo("# Idea");

        Files.writeString(root().resolve("occupied.md"), "untouched");
        assertError(move("renamed.md", "occupied.md", quote(revision)), 409, "PATH_EXISTS");
        assertThat(Files.readString(root().resolve("renamed.md"))).isEqualTo("# Idea");
        assertThat(Files.readString(root().resolve("occupied.md"))).isEqualTo("untouched");
    }

    @Test
    void movesNestedFolderAndChangesRevisionWithNewDescendantPaths() throws Exception {
        Files.createDirectories(root().resolve("Notes/Nested"));
        Files.writeString(root().resolve("Notes/Nested/idea.md"), "# Idea");
        String oldRevision = folderRevision("Notes");
        var moved = move("Notes", "Archive", quote(oldRevision));

        assertThat(moved.statusCode()).isEqualTo(200);
        assertThat(data(moved).get("path").asText()).isEqualTo("Archive");
        assertThat(data(moved).get("revision").asText()).isNotEqualTo(oldRevision);
        assertThat(data(moved).get("children").get(0).get("path").asText()).isEqualTo("Archive/Nested");
        assertThat(data(moved).get("children").get(0).get("children").get(0).get("path").asText())
                .isEqualTo("Archive/Nested/idea.md");
        assertThat(Files.exists(root().resolve("Notes"))).isFalse();
        assertThat(Files.readString(root().resolve("Archive/Nested/idea.md"))).isEqualTo("# Idea");
    }

    @Test
    void staleFolderRevisionAndDestinationCollisionLeaveSourceIntact() throws Exception {
        Files.createDirectory(root().resolve("Notes"));
        String stale = folderRevision("Notes");
        Files.writeString(root().resolve("Notes/new.md"), "new");
        assertError(move("Notes", "Archive", quote(stale)), 412, "REVISION_CONFLICT");
        assertError(delete("Notes", quote(stale)), 412, "REVISION_CONFLICT");
        assertThat(Files.readString(root().resolve("Notes/new.md"))).isEqualTo("new");

        Files.createDirectory(root().resolve("Archive"));
        assertError(move("Notes", "Archive", quote(folderRevision("Notes"))), 409, "PATH_EXISTS");
        assertThat(Files.isDirectory(root().resolve("Notes"))).isTrue();
        assertThat(Files.isDirectory(root().resolve("Archive"))).isTrue();
        assertError(move("Notes", "Notes", quote(folderRevision("Notes"))), 409, "PATH_EXISTS");
    }

    @Test
    void staleFileRevisionCannotMoveOrDeleteCurrentContent() throws Exception {
        Files.writeString(root().resolve("idea.md"), "old");
        String stale = fileRevision("idea.md");
        Files.writeString(root().resolve("idea.md"), "new");
        assertError(move("idea.md", "renamed.md", quote(stale)), 412, "REVISION_CONFLICT");
        assertError(delete("idea.md", quote(stale)), 412, "REVISION_CONFLICT");
        assertThat(Files.readString(root().resolve("idea.md"))).isEqualTo("new");
        assertThat(Files.exists(root().resolve("renamed.md"))).isFalse();
    }

    @Test
    void permanentlyDeletesFileAndNestedFolderWithoutFollowingSymlinks() throws Exception {
        Path outside = temporaryDirectory.resolve("outside");
        Files.createDirectories(outside);
        Files.writeString(outside.resolve("private.md"), "outside secret");
        Files.createDirectories(root().resolve("Notes/Nested"));
        Files.writeString(root().resolve("Notes/Nested/idea.md"), "# Idea");
        Files.createSymbolicLink(root().resolve("Notes/Nested/link.md"), outside.resolve("private.md"));

        var deletedFile = delete("Notes/Nested/idea.md", quote(fileRevision("Notes/Nested/idea.md")));
        assertThat(deletedFile.statusCode()).isEqualTo(204);
        assertThat(deletedFile.body()).isEmpty();
        assertThat(Files.exists(root().resolve("Notes/Nested/idea.md"))).isFalse();
        var deletedFolder = delete("Notes", quote(folderRevision("Notes")));
        assertThat(deletedFolder.statusCode()).isEqualTo(204);
        assertThat(deletedFolder.body()).isEmpty();
        assertThat(Files.exists(root().resolve("Notes"))).isFalse();
        assertThat(Files.readString(outside.resolve("private.md"))).isEqualTo("outside secret");
    }

    @Test
    void rejectsMissingOrMalformedPreconditionsAndMissingSources() throws Exception {
        Files.writeString(root().resolve("idea.md"), "# Idea");
        assertError(move("idea.md", "new.md", null), 400, "PRECONDITION_REQUIRED");
        assertError(delete("idea.md", null), 400, "PRECONDITION_REQUIRED");
        assertError(move("idea.md", "new.md", "bad"), 400, "INVALID_REQUEST");
        assertError(delete("idea.md", "bad"), 400, "INVALID_REQUEST");
        assertError(move("missing.md", "new.md", quote("0".repeat(64))), 404, "NOT_FOUND");
        assertError(delete("missing.md", quote("0".repeat(64))), 404, "NOT_FOUND");
        assertError(move("idea.md", "Missing/new.md", quote(fileRevision("idea.md"))), 404, "NOT_FOUND");
        assertThat(Files.readString(root().resolve("idea.md"))).isEqualTo("# Idea");
    }

    @Test
    void rejectsVaultRootUnsafeDescendantsAndSymlinkPaths() throws Exception {
        Files.createDirectories(root().resolve("Notes/Nested"));
        Path outside = temporaryDirectory.resolve("outside-dir");
        Files.createDirectories(outside);
        Files.createSymbolicLink(root().resolve("escape"), outside);
        Files.createSymbolicLink(root().resolve("escape.md"), outside.resolve("private.md"));
        String revision = quote(folderRevision("Notes"));

        assertError(delete("", revision), 400, "INVALID_PATH");
        assertError(delete("/", revision), 400, "INVALID_PATH");
        assertError(move("Notes", "Notes/Nested/Child", revision), 400, "INVALID_PATH");
        assertError(move("Notes", "escape/Child", revision), 400, "INVALID_PATH");
        assertError(folder("escape/Child"), 400, "INVALID_PATH");
        assertError(delete("escape", revision), 400, "INVALID_PATH");
        assertError(move("escape.md", "safe.md", quote("0".repeat(64))), 400, "INVALID_PATH");
        assertError(delete("escape.md", quote("0".repeat(64))), 400, "INVALID_PATH");
        assertThat(Files.isDirectory(root().resolve("Notes"))).isTrue();
        try (var entries = Files.list(outside)) {
            assertThat(entries.count()).isZero();
        }
    }

    private HttpResponse<String> folder(String path) throws Exception {
        return post("/folders", Map.of("path", path), null);
    }

    private HttpResponse<String> move(String from, String to, String ifMatch) throws Exception {
        return post("/moves", Map.of("from", from, "to", to), ifMatch);
    }

    private HttpResponse<String> post(String suffix, Map<String, String> body, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(suffix))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (ifMatch != null) request.header("If-Match", ifMatch);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> delete(String path, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri("/items?path="
                + URLEncoder.encode(path, StandardCharsets.UTF_8))).DELETE();
        if (ifMatch != null) request.header("If-Match", ifMatch);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String fileRevision(String path) throws Exception {
        return json.readTree(client.send(HttpRequest.newBuilder(uri("/documents?path="
                + URLEncoder.encode(path, StandardCharsets.UTF_8))).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body())
                .get("data").get("revision").asText();
    }

    private String folderRevision(String path) throws Exception {
        JsonNode tree = json.readTree(client.send(HttpRequest.newBuilder(uri("/tree")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body()).get("data");
        return findNode(tree, path).get("revision").asText();
    }

    private JsonNode findNode(JsonNode nodes, String path) {
        for (JsonNode node : nodes) {
            if (node.get("path").asText().equals(path)) return node;
            if (node.has("children")) {
                JsonNode found = findNode(node.get("children"), path);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JsonNode data(HttpResponse<String> response) {
        return json.readTree(response.body()).get("data");
    }

    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo(code);
        assertThat(body.get("error").get("details")).isEmpty();
        assertThat(response.body()).doesNotContain(root().toString());
    }

    private URI uri(String suffix) {
        return URI.create("http://127.0.0.1:" + port + "/api/v1/vault" + suffix);
    }

    private static String quote(String revision) {
        return '"' + revision + '"';
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
