package io.github.nikhil8bph.markcraft.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "markcraft.vault")
public record VaultProperties(Path root) {
}
