package io.github.nikhil8bph.markcraft.exception;

import org.springframework.http.HttpStatus;

public final class VaultApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public VaultApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
