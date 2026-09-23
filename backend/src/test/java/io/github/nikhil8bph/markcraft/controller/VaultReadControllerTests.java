package io.github.nikhil8bph.markcraft.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
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
class VaultReadControllerTests {

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
    void clearVault() throws IOException {
        try (var entries = Files.walk(root())) {
            for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(root())) Files.delete(path);
            }
        }
    }

    @Test
    void emptyVaultReturnsEmptyTree() throws Exception {
        var response = get("/tree");
        JsonNode body = json.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("data").isArray()).isTrue();
        assertThat(body.get("data")).isEmpty();
        assertThat(Instant.parse(body.get("timestamp").asText())).isNotNull();
    }

    @Test
    void nestedTreeHasNaturalOrderMetadataAndContentRevisions() throws Exception {
        Files.createDirectories(root().resolve("Folder10/nested"));
        Files.createDirectories(root().resolve("Folder2"));
        Files.writeString(root().resolve("Folder10/nested/Note10.md"), "# Ten\n");
        Files.writeString(root().resolve("Folder10/nested/Note2.md"), "# Two\n");
        Files.writeString(root().resolve("Root.md"), "Root");
        Files.writeString(root().resolve("ignore.txt"), "ignored");
        Files.writeString(root().resolve(".hidden.md"), "ignored");
        Files.createSymbolicLink(root().resolve("linked.md"), root().resolve("Root.md"));

        JsonNode nodes = json.readTree(get("/tree").body()).get("data");
        assertThat(nodes).hasSize(3);
        assertThat(nodes.get(0).get("path").asText()).isEqualTo("Folder2");
        assertThat(nodes.get(1).get("path").asText()).isEqualTo("Folder10");
        assertThat(nodes.get(2).get("path").asText()).isEqualTo("Root.md");
        assertThat(nodes.get(0).has("size")).isFalse();
        assertThat(nodes.get(0).get("children")).isEmpty();
        JsonNode nested = nodes.get(1).get("children").get(0);
        assertThat(nested.get("children").get(0).get("name").asText()).isEqualTo("Note2.md");
        assertThat(nested.get("children").get(1).get("name").asText()).isEqualTo("Note10.md");
        assertThat(nodes.get(2).get("revision").asText()).isEqualTo(sha256("Root"));
        assertThat(nodes.get(2).get("size").asLong()).isEqualTo(4);
        assertThat(nodes.get(2).has("children")).isFalse();
        assertThat(Instant.parse(nodes.get(2).get("updatedAt").asText())).isNotNull();

        String before = nodes.get(1).get("revision").asText();
        Files.writeString(root().resolve("Folder10/nested/Note2.md"), "# Changed\n");
        JsonNode after = json.readTree(get("/tree").body()).get("data");
        assertThat(after.get(1).get("revision").asText()).isNotEqualTo(before);
        assertThat(after.get(0).get("revision").asText()).hasSize(64);
    }

    @Test
    void readReturnsStoredUtf8BytesRevisionAndQuotedEtag() throws Exception {
        Files.createDirectory(root().resolve("Notes"));
        String content = "# Café 📄\n";
        Files.writeString(root().resolve("Notes/idea.md"), content);

        var response = read("Notes/idea.md");
        JsonNode document = json.readTree(response.body()).get("data");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(document.get("path").asText()).isEqualTo("Notes/idea.md");
        assertThat(document.get("name").asText()).isEqualTo("idea.md");
        assertThat(document.get("content").asText()).isEqualTo(content);
        assertThat(document.get("size").asLong()).isEqualTo(content.getBytes(StandardCharsets.UTF_8).length);
        assertThat(document.get("revision").asText()).isEqualTo(sha256(content));
        assertThat(response.headers().firstValue("ETag")).contains('"' + sha256(content) + '"');
        assertThat(Instant.parse(document.get("updatedAt").asText())).isNotNull();
    }

    @Test
    void rejectsTraversalAbsoluteHiddenAndNonMarkdownPaths() throws Exception {
        for (String path : new String[] {"../outside.md", "Notes/../../outside.md", "/tmp/outside.md",
                "Notes\\idea.md", ".hidden.md", "Notes/.hidden.md", "Notes//idea.md",
                "Notes/./idea.md", "Notes/idea.txt", "CON.md", "Notes/", "Folder.md/idea.md"}) {
            var response = read(path);
            assertThat(response.statusCode()).as(path).isEqualTo(400);
            JsonNode body = json.readTree(response.body());
            assertThat(body.get("error").get("code").asText()).isEqualTo("INVALID_PATH");
            assertThat(response.body()).doesNotContain(root().toString());
        }
    }

    @Test
    void rejectsSymlinkEscapesAndDoesNotExposeOutsideContent() throws Exception {
        Path outside = temporaryDirectory.resolve("vault-other");
        Files.createDirectories(outside);
        Files.writeString(outside.resolve("private.md"), "outside secret");
        Files.createSymbolicLink(root().resolve("outside"), outside);
        Files.createSymbolicLink(root().resolve("private.md"), outside.resolve("private.md"));

        for (String path : new String[] {"outside/private.md", "private.md", "../vault-other/private.md"}) {
            var response = read(path);
            assertThat(response.statusCode()).as(path).isEqualTo(400);
            assertThat(response.body()).doesNotContain("outside secret", outside.toString());
        }
        assertThat(json.readTree(get("/tree").body()).get("data")).isEmpty();
    }

    @Test
    void missingDocumentAndMalformedUtf8UseContractErrorEnvelope() throws Exception {
        var absent = read("missing.md");
        assertThat(absent.statusCode()).isEqualTo(404);
        assertThat(json.readTree(absent.body()).get("error").get("code").asText()).isEqualTo("NOT_FOUND");

        Files.write(root().resolve("broken.md"), new byte[] {(byte) 0xc3, (byte) 0x28});
        var malformed = read("broken.md");
        assertThat(malformed.statusCode()).isEqualTo(500);
        assertThat(json.readTree(malformed.body()).get("error").get("code").asText())
                .isEqualTo("STORAGE_FAILURE");
        assertThat(malformed.body()).doesNotContain(root().toString());

        var noPath = get("/documents");
        assertThat(noPath.statusCode()).isEqualTo(400);
        assertThat(json.readTree(noPath.body()).get("error").get("code").asText())
                .isEqualTo("INVALID_REQUEST");
    }

    @Test
    void rejectsNonLoopbackHostBeforeServingVaultData() throws Exception {
        Files.writeString(root().resolve("private.md"), "# Private");
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.getOutputStream().write(("GET /api/v1/vault/tree HTTP/1.1\r\n"
                    + "Host: evil.example\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(response).contains("400", "INVALID_REQUEST");
            assertThat(response).doesNotContain("private.md", "# Private");
        }
    }

    private HttpResponse<String> read(String path) throws Exception {
        return get("/documents?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String suffix) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/vault" + suffix))
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
