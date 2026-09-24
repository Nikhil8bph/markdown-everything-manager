package io.github.nikhil8bph.markcraft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "markcraft.mcp")
public record McpProperties(boolean enabled, String token, boolean agentDeletionEnabled) {

    public McpProperties {
        if (enabled && (token == null || token.isBlank())) {
            throw new IllegalStateException("MCP requires a nonempty markcraft.mcp.token");
        }
    }

    @Override
    public String toString() {
        return "McpProperties[enabled=" + enabled + ", token=<redacted>, agentDeletionEnabled=" + agentDeletionEnabled + "]";
    }
}
