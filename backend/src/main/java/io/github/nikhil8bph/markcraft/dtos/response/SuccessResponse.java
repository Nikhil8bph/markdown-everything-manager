package io.github.nikhil8bph.markcraft.dtos.response;

import java.time.Instant;

public record SuccessResponse<T>(boolean success, T data, Instant timestamp) {

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(true, data, Instant.now());
    }
}
