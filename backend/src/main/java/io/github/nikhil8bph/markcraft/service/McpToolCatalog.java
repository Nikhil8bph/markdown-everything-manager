package io.github.nikhil8bph.markcraft.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class McpToolCatalog {

    private final ObjectMapper objectMapper;
    private final JsonNode catalog;

    public McpToolCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try (var input = new ClassPathResource("contracts/mcp/v1-tools.json").getInputStream()) {
            this.catalog = objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the MCP tool catalog", exception);
        }
    }

    public JsonNode tools() {
        return catalog.path("x-tools");
    }

    public Map<String, Object> resolvedSchema(JsonNode schemaReference) {
        String reference = schemaReference.path("$ref").asText();
        String definition = reference.startsWith("#/$defs/") ? reference.substring("#/$defs/".length()) : "";
        JsonNode schemaDefinition = catalog.path("$defs").path(definition);
        if (definition.isBlank() || schemaDefinition.isMissingNode()) {
            throw new IllegalStateException("MCP catalog contains an unresolved schema reference");
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", catalog.path("$schema").asText());
        schema.put("$defs", objectMapper.convertValue(catalog.path("$defs"), Map.class));
        schema.putAll(objectMapper.convertValue(schemaDefinition, Map.class));
        return schema;
    }
}
