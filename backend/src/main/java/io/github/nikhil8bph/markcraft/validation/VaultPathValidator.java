package io.github.nikhil8bph.markcraft.validation;

import io.github.nikhil8bph.markcraft.config.VaultProperties;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class VaultPathValidator {

    private static final Set<String> RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private final VaultProperties properties;

    public VaultPathValidator(VaultProperties properties) {
        this.properties = properties;
    }

    public Path root() {
        Path configured = properties.root().toAbsolutePath().normalize();
        if (Files.isSymbolicLink(configured)) {
            throw invalidPath();
        }
        try {
            return configured.toRealPath();
        } catch (IOException exception) {
            throw storageFailure();
        }
    }

    public Path document(String relativePath) {
        return resolve(relativePath, true);
    }

    public Path folder(String relativePath) {
        return resolve(relativePath, false);
    }

    public Path itemPath(String relativePath, boolean file) {
        return resolve(relativePath, file);
    }

    private Path resolve(String relativePath, boolean document) {
        if (relativePath == null || relativePath.isEmpty() || relativePath.length() > 1024
                || relativePath.startsWith("/") || relativePath.contains("\\")) {
            throw invalidPath();
        }
        String[] segments = relativePath.split("/", -1);
        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            if (!isVisibleEntryName(segment)
                    || (index < segments.length - 1 && segment.toLowerCase(Locale.ROOT).endsWith(".md"))) {
                throw invalidPath();
            }
        }
        String leaf = segments[segments.length - 1].toLowerCase(Locale.ROOT);
        if (document != leaf.endsWith(".md")) {
            throw invalidPath();
        }

        Path root = root();
        Path resolved = root;
        for (String segment : segments) {
            resolved = resolved.resolve(segment);
            if (Files.isSymbolicLink(resolved)) {
                throw invalidPath();
            }
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw invalidPath();
        }
        return resolved;
    }

    public boolean isVisibleEntryName(String name) {
        if (name == null || name.isEmpty() || name.equals(".") || name.equals("..")
                || name.startsWith(".") || name.endsWith(".") || name.endsWith(" ")
                || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            return false;
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isISOControl(character) || Character.isSurrogate(character)
                    || "<>:\"|?*".indexOf(character) >= 0) {
                return false;
            }
        }
        String stem = name.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
        return !RESERVED_NAMES.contains(stem);
    }

    public static VaultApiException invalidPath() {
        return new VaultApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "Invalid vault path");
    }

    public static VaultApiException storageFailure() {
        return new VaultApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FAILURE", "Vault storage is unavailable");
    }

    public static VaultApiException notFound() {
        return new VaultApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Vault item was not found");
    }
}
