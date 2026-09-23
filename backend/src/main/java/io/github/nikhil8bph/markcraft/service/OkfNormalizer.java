package io.github.nikhil8bph.markcraft.service;

import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import java.io.StringReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

@Component
public class OkfNormalizer {

    private static final Pattern CLOSING_DELIMITER = Pattern.compile("(?m)^---[ \\t]*\\r?$");
    private static final Pattern FIRST_HEADING = Pattern.compile("(?m)^#[ \\t]+([^\\r\\n]+)");

    public String normalize(String content, String filename) {
        int openingLength = openingDelimiterLength(content);
        if (openingLength == 0) {
            return prependMetadata(content, filename);
        }
        Matcher closing = CLOSING_DELIMITER.matcher(content);
        if (!closing.find(openingLength)) {
            throw invalidFrontmatter();
        }
        String frontmatter = content.substring(openingLength, closing.start());
        Yaml yaml = safeYaml();
        try {
            Object parsed = yaml.load(frontmatter);
            if (parsed != null && !(parsed instanceof Map<?, ?>)) {
                throw invalidFrontmatter();
            }
            Map<?, ?> values = parsed == null ? Map.of() : (Map<?, ?>) parsed;
            if (!values.containsKey("type")) {
                Node node = yaml.compose(new StringReader(frontmatter));
                if (node instanceof MappingNode mapping && mapping.getFlowStyle() == DumperOptions.FlowStyle.FLOW) {
                    int start = utf16Offset(frontmatter, mapping.getStartMark().getIndex());
                    int brace = frontmatter.indexOf('{', start);
                    if (brace < 0) throw invalidFrontmatter();
                    String addition = mapping.getValue().isEmpty() ? "type: concept" : "type: concept, ";
                    return content.substring(0, openingLength + brace + 1) + addition
                            + content.substring(openingLength + brace + 1);
                }
                String newline = openingLength == 5 ? "\r\n" : "\n";
                return content.substring(0, openingLength) + "type: concept" + newline
                        + content.substring(openingLength);
            }
            Object type = values.get("type");
            if (type instanceof String text && !text.isBlank()) {
                return content;
            }
            Node root = yaml.compose(new StringReader(frontmatter));
            if (!(root instanceof MappingNode mapping)) {
                throw invalidFrontmatter();
            }
            NodeTuple typeEntry = mapping.getValue().stream()
                    .filter(tuple -> tuple.getKeyNode() instanceof ScalarNode scalar
                            && scalar.getValue().equals("type"))
                    .findFirst().orElseThrow(OkfNormalizer::invalidFrontmatter);
            int start = utf16Offset(frontmatter, typeEntry.getKeyNode().getStartMark().getIndex());
            int end = utf16Offset(frontmatter, typeEntry.getValueNode().getEndMark().getIndex());
            while (end < frontmatter.length()
                    && (frontmatter.charAt(end) == ' ' || frontmatter.charAt(end) == '\t')) {
                end++;
            }
            String replacement = "type: concept";
            if (end < frontmatter.length() && frontmatter.charAt(end) == '#') {
                replacement += " ";
            } else if (end > 0 && frontmatter.charAt(end - 1) == '\n') {
                replacement += end > 1 && frontmatter.charAt(end - 2) == '\r' ? "\r\n" : "\n";
            }
            return content.substring(0, openingLength + start) + replacement
                    + content.substring(openingLength + end);
        } catch (YAMLException | IllegalArgumentException exception) {
            throw invalidFrontmatter();
        }
    }

    private static int openingDelimiterLength(String content) {
        if (content.startsWith("---\r\n")) return 5;
        if (content.startsWith("---\n")) return 4;
        if (content.equals("---")) throw invalidFrontmatter();
        return 0;
    }

    private static String prependMetadata(String content, String filename) {
        Matcher heading = FIRST_HEADING.matcher(content);
        String title = heading.find() ? heading.group(1).trim() : filename.substring(0, filename.length() - 3);
        String safeTitle = title.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\t", "\\t");
        return "---\ntype: concept\ntitle: \"" + safeTitle + "\"\n---\n\n" + content;
    }

    private static int utf16Offset(String text, int yamlCodePointOffset) {
        if (yamlCodePointOffset < 0 || yamlCodePointOffset > text.codePointCount(0, text.length())) {
            throw invalidFrontmatter();
        }
        return text.offsetByCodePoints(0, yamlCodePointOffset);
    }

    private static Yaml safeYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setCodePointLimit(25_000_000);
        return new Yaml(new SafeConstructor(options));
    }

    private static VaultApiException invalidFrontmatter() {
        return new VaultApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FRONTMATTER",
                "Markdown frontmatter must be a valid YAML mapping");
    }
}
