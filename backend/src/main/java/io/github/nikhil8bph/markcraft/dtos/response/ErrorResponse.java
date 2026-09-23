package io.github.nikhil8bph.markcraft.dtos.response;

import java.time.Instant;

public record ErrorResponse(boolean success, ErrorInfo error, Instant timestamp) {

    public static ErrorResponse of(ErrorInfo error) {
        return new ErrorResponse(false, error, Instant.now());
    }
}
