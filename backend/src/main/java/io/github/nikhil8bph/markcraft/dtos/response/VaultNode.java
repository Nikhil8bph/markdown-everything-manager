package io.github.nikhil8bph.markcraft.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VaultNode(String name, String path, String type, Instant updatedAt, String revision,
                        Long size, List<VaultNode> children) {
}
