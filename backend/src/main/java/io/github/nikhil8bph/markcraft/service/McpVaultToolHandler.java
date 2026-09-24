package io.github.nikhil8bph.markcraft.service;

import io.github.nikhil8bph.markcraft.dtos.response.ErrorDetail;
import io.github.nikhil8bph.markcraft.dtos.response.ErrorInfo;
import io.github.nikhil8bph.markcraft.dtos.response.SuccessResponse;
import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.request.UploadRequest;
import io.github.nikhil8bph.markcraft.dtos.response.UploadResponse;
import io.github.nikhil8bph.markcraft.config.McpProperties;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class McpVaultToolHandler {

    private final VaultReadService readService;
    private final VaultWriteService writeService;
    private final VaultItemService itemService;
    private final VaultUploadService uploadService;
    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper;

    public McpVaultToolHandler(VaultReadService readService, VaultWriteService writeService,
            VaultItemService itemService, VaultUploadService uploadService, McpProperties mcpProperties,
            ObjectMapper objectMapper) {
        this.readService = readService;
        this.writeService = writeService;
        this.itemService = itemService;
        this.uploadService = uploadService;
        this.mcpProperties = mcpProperties;
        this.objectMapper = objectMapper;
    }

    public CallToolResult call(CallToolRequest request) {
        try {
            Object result = switch (request.name()) {
                case "get_vault_tree" -> {
                    requireEmptyArguments(request.arguments());
                    yield success(readService.tree());
                }
                case "get_document" -> {
                    requireFields(request.arguments(), Set.of("path"));
                    yield success(readService.document(path(request.arguments())));
                }
                case "put_document" -> putDocument(request.arguments());
                case "create_folder" -> {
                    requireFields(request.arguments(), Set.of("path"));
                    yield success(itemService.createFolder(path(request.arguments())));
                }
                case "upload_documents" -> uploadDocuments(request.arguments());
                case "move_item" -> moveItem(request.arguments());
                case "delete_item" -> deleteItem(request.arguments());
                default -> throw invalid("name", "Unknown MCP tool");
            };
            return result(result);
        } catch (VaultApiException exception) {
            return error(new ErrorInfo(exception.code(), exception.getMessage(), List.of()));
        } catch (InvalidToolRequest exception) {
            return error(new ErrorInfo("INVALID_REQUEST", exception.getMessage(),
                    List.of(new ErrorDetail(exception.field, exception.getMessage()))));
        } catch (ToolOperationException exception) {
            return error(new ErrorInfo(exception.code, exception.getMessage(), List.of()));
        } catch (RuntimeException exception) {
            return error(new ErrorInfo("INTERNAL_ERROR", "The vault operation could not be completed", List.of()));
        }
    }

    private UploadResponse uploadDocuments(Map<String, Object> arguments) {
        requireFields(arguments, Set.of("folder", "files"));
        String folder = stringField(arguments, "folder", true);
        Object rawFiles = arguments.get("files");
        if (!(rawFiles instanceof List<?> files) || files.isEmpty()) {
            throw invalid("files", "files must be a nonempty array");
        }
        List<UploadFileRequest> requests = new ArrayList<>(files.size());
        for (int index = 0; index < files.size(); index++) {
            Map<String, Object> file = objectFields(files.get(index), "files[" + index + "]");
            Set<String> allowed = Set.of("name", "content", "expectedRevision");
            if (!file.keySet().containsAll(Set.of("name", "content")) || !allowed.containsAll(file.keySet())) {
                throw invalid("files[" + index + "]", "Each file requires name and content with only an optional expectedRevision");
            }
            String name = stringField(file, "name", false);
            String content = stringValue(file.get("content"), "files[" + index + "].content", true);
            Object expectedRevision = file.get("expectedRevision");
            if (expectedRevision != null && !(expectedRevision instanceof String)) {
                throw invalid("files[" + index + "].expectedRevision", "expectedRevision must be a revision string or null");
            }
            requests.add(new UploadFileRequest(name, content, (String) expectedRevision));
        }
        return uploadService.upload(new UploadRequest(folder, List.copyOf(requests)));
    }

    private SuccessResponse<?> moveItem(Map<String, Object> arguments) {
        requireFields(arguments, Set.of("from", "to", "expectedRevision"));
        String from = requiredString(arguments, "from");
        String to = requiredString(arguments, "to");
        String revision = requiredString(arguments, "expectedRevision");
        return success(itemService.move(from, to, quoteRevision(revision)));
    }

    private SuccessResponse<?> deleteItem(Map<String, Object> arguments) {
        requireFields(arguments, Set.of("path", "expectedRevision"));
        String path = requiredString(arguments, "path");
        String revision = requiredString(arguments, "expectedRevision");
        if (!mcpProperties.agentDeletionEnabled()) {
            throw new ToolOperationException("DELETE_DISABLED", "Agent deletion is disabled");
        }
        itemService.delete(path, quoteRevision(revision));
        return success(Map.of("path", path, "deleted", true));
    }

    private SuccessResponse<?> putDocument(Map<String, Object> arguments) {
        Set<String> required = Set.of("path", "content");
        Set<String> allowed = Set.of("path", "content", "expectedRevision");
        if (arguments == null || !arguments.keySet().containsAll(required)
                || !allowed.containsAll(arguments.keySet())) {
            throw invalid("arguments", "put_document requires path and content, with only an optional expectedRevision");
        }
        String path = requiredString(arguments, "path");
        String content = requiredString(arguments, "content");
        // MCP SDK 2.x drops explicit null map values; an omitted revision is therefore its create-only null.
        Object expectedRevision = arguments.get("expectedRevision");
        if (expectedRevision != null && !(expectedRevision instanceof String)) {
            throw invalid("expectedRevision", "expectedRevision must be a revision string or null");
        }
        String revision = (String) expectedRevision;
        return SuccessResponse.of(writeService.save(path, content,
                revision == null ? null : quoteRevision(revision), revision == null ? "*" : null));
    }

    private SuccessResponse<?> success(Object data) {
        return SuccessResponse.of(data);
    }

    private String path(Map<String, Object> arguments) {
        return requiredString(arguments, "path");
    }

    private String requiredString(Map<String, Object> arguments, String field) {
        if (arguments == null) {
            throw invalid(field, field + " is required");
        }
        Object value = arguments.get(field);
        if (!(value instanceof String string) || !StringUtils.hasText(string)) {
            throw invalid(field, field + " must be a nonempty string");
        }
        return string;
    }

    private String stringField(Map<String, Object> arguments, String field, boolean allowEmpty) {
        return stringValue(arguments.get(field), field, allowEmpty);
    }

    private String stringValue(Object value, String field, boolean allowEmpty) {
        if (!(value instanceof String string) || (!allowEmpty && !StringUtils.hasText(string))) {
            throw invalid(field, field + (allowEmpty ? " must be a string" : " must be a nonempty string"));
        }
        return string;
    }

    private Map<String, Object> objectFields(Object value, String field) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw invalid(field, field + " must be an object");
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw invalid(field, field + " contains an invalid property name");
            }
            fields.put(key, entry.getValue());
        }
        return fields;
    }

    private String quoteRevision(String revision) {
        return "\"" + revision + "\"";
    }

    private void requireFields(Map<String, Object> arguments, Set<String> expectedFields) {
        if (arguments == null || !arguments.keySet().equals(expectedFields)) {
            throw invalid("arguments", "Arguments must contain exactly " + expectedFields);
        }
    }

    private void requireEmptyArguments(Map<String, Object> arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            throw invalid("arguments", "get_vault_tree does not accept arguments");
        }
    }

    private InvalidToolRequest invalid(String field, String message) {
        return new InvalidToolRequest(field, message);
    }

    private CallToolResult result(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            Map<String, Object> structured = objectMapper.convertValue(value, Map.class);
            return CallToolResult.builder().structuredContent(structured).addTextContent(json).isError(false).build();
        } catch (JacksonException exception) {
            return CallToolResult.builder()
                    .addTextContent("{\"code\":\"INTERNAL_ERROR\",\"message\":\"The response could not be encoded\",\"details\":[]}")
                    .isError(true).build();
        }
    }

    private CallToolResult error(ErrorInfo error) {
        try {
            return CallToolResult.builder().addTextContent(objectMapper.writeValueAsString(error)).isError(true).build();
        } catch (JacksonException exception) {
            return CallToolResult.builder()
                    .addTextContent("{\"code\":\"INTERNAL_ERROR\",\"message\":\"The response could not be encoded\",\"details\":[]}")
                    .isError(true).build();
        }
    }

    private static final class ToolOperationException extends RuntimeException {
        private final String code;

        private ToolOperationException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static final class InvalidToolRequest extends RuntimeException {
        private final String field;

        private InvalidToolRequest(String field, String message) {
            super(message);
            this.field = field;
        }
    }
}
