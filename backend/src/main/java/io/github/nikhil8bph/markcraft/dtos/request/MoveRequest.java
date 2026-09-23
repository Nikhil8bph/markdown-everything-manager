package io.github.nikhil8bph.markcraft.dtos.request;

import jakarta.validation.constraints.NotNull;

public record MoveRequest(@NotNull String from, @NotNull String to) {
}
