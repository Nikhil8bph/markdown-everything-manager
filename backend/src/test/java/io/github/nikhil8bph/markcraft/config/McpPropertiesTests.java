package io.github.nikhil8bph.markcraft.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

class McpPropertiesTests {

    @Test
    void enabledMcpRequiresANonemptyToken() {
        assertThatThrownBy(() -> new McpProperties(true, null, false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("markcraft.mcp.token");
        assertThatThrownBy(() -> new McpProperties(true, "  ", false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("markcraft.mcp.token");
        assertThat(new McpProperties(false, null, false).enabled()).isFalse();
    }

    @Test
    void tokenIsRedactedFromConfigurationRepresentation() {
        assertThat(new McpProperties(true, "secret-value", false).toString())
                .contains("<redacted>").doesNotContain("secret-value");
    }

    @Test
    void startupBindingRejectsEnabledMcpWithoutAToken() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("markcraft.mcp.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(McpProperties.class)
    static class PropertiesConfiguration {
    }
}
