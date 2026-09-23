package io.github.nikhil8bph.markcraft;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikhil8bph.markcraft.config.VaultProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MarkcraftApplicationTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void vaultRoot(DynamicPropertyRegistry registry) {
        registry.add("markcraft.vault.root", () -> tempDir.resolve("vault").toString());
    }

    @Autowired
    private VaultProperties vaultProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private RequestMappingHandlerMapping mappings;

    @Test
    void startsOnLoopbackWithAnEmptyVaultAndOnlyImplementedContractedRoutes() throws Exception {
        assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(Files.isDirectory(vaultProperties.root())).isTrue();
        try (var entries = Files.list(vaultProperties.root())) {
            assertThat(entries).isEmpty();
        }
        assertThat(mappings.getHandlerMethods().keySet().stream()
                .map(Object::toString)
                .filter(mapping -> mapping.contains("/api/v1/vault")))
                .containsExactlyInAnyOrder("{GET [/api/v1/vault/tree]}", "{GET [/api/v1/vault/documents]}",
                        "{PUT [/api/v1/vault/documents]}", "{POST [/api/v1/vault/folders]}",
                        "{POST [/api/v1/vault/moves]}", "{DELETE [/api/v1/vault/items]}",
                        "{POST [/api/v1/vault/uploads]}");
    }
}
