package io.github.nikhil8bph.markcraft.exception;

import io.github.nikhil8bph.markcraft.dtos.response.ErrorInfo;
import io.github.nikhil8bph.markcraft.dtos.response.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class VaultApiExceptionHandler {

    @ExceptionHandler(VaultApiException.class)
    public ResponseEntity<ErrorResponse> handleVaultApiException(VaultApiException exception) {
        return ResponseEntity.status(exception.status())
                .body(ErrorResponse.of(new ErrorInfo(exception.code(), exception.getMessage(), List.of())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(new ErrorInfo("INVALID_REQUEST", "Required request parameter is missing", List.of())));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidBody() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(new ErrorInfo("INVALID_REQUEST", "Invalid request body", List.of())));
    }
}
