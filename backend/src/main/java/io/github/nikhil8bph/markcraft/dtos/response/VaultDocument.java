package io.github.nikhil8bph.markcraft.dtos.response;

import java.time.Instant;

public record VaultDocument(String path, String name, String content, Instant updatedAt, long size, String revision) {
}
