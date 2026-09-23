package io.github.nikhil8bph.markcraft.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UploadFileRequest(@NotNull String name, @NotNull String content,
                                @JsonProperty(value = "expectedRevision", required = true) String expectedRevision) {
}
