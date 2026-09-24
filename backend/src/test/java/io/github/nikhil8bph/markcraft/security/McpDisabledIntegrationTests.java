package io.github.nikhil8bph.markcraft.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpDisabledIntegrationTests {

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
    void disabledMcpDoesNotFallThroughToAngular() throws Exception {
        for (String method : new String[] {"GET", "POST"}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                    .method(method, HttpRequest.BodyPublishers.noBody()).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(method).isEqualTo(404);
            assertThat(response.body()).doesNotContain("<app-root");
        }
    }
}
