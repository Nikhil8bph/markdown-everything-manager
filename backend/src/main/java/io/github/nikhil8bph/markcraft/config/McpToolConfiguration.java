package io.github.nikhil8bph.markcraft.config;

import io.github.nikhil8bph.markcraft.service.McpToolCatalog;
import io.github.nikhil8bph.markcraft.service.McpVaultToolHandler;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;

@Configuration
public class McpToolConfiguration {

    private static final List<String> REGISTERED_TOOLS = List.of(
            "get_vault_tree", "get_document", "put_document", "create_folder",
            "upload_documents", "move_item", "delete_item");

    @Bean
    @Primary
    McpSyncServerCustomizer markCraftMcpServerCustomizer() {
        return specification -> specification.validateToolInputs(false).immediateExecution(true);
    }

    @Bean
    List<SyncToolSpecification> markCraftMcpTools(McpToolCatalog catalog, McpVaultToolHandler handler) {
        List<SyncToolSpecification> tools = new ArrayList<>();
        for (String name : REGISTERED_TOOLS) {
            JsonNode definition = findTool(catalog, name);
            Tool tool = Tool.builder()
                    .name(name)
                    .description(definition.path("description").asText())
                    .inputSchema(catalog.resolvedSchema(definition.path("inputSchema")))
                    .outputSchema(catalog.resolvedSchema(definition.path("outputSchema")))
                    .annotations(annotations(definition.path("annotations")))
                    .build();
            tools.add(SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> handler.call(request)).build());
        }
        return List.copyOf(tools);
    }

    private JsonNode findTool(McpToolCatalog catalog, String name) {
        for (JsonNode definition : catalog.tools()) {
            if (name.equals(definition.path("name").asText())) {
                return definition;
            }
        }
        throw new IllegalStateException("MCP catalog is missing tool " + name);
    }

    private ToolAnnotations annotations(JsonNode annotations) {
        return ToolAnnotations.builder()
                .readOnlyHint(annotations.path("readOnlyHint").asBoolean())
                .destructiveHint(annotations.path("destructiveHint").asBoolean())
                .idempotentHint(annotations.path("idempotentHint").asBoolean())
                .openWorldHint(annotations.path("openWorldHint").asBoolean())
                .build();
    }
}
