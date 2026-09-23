package io.github.nikhil8bph.markcraft.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UploadRequest(@NotNull String folder, @NotEmpty List<@NotNull @Valid UploadFileRequest> files) {
}
