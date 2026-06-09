package com.re.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation Error");

        log.error("VALIDATION ERROR : {}", msg);
        return ResponseEntity
                .badRequest()
                .body(buildError(400, "BAD_REQUEST", msg, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> global(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("SYSTEM ERROR", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(500, "SERVER_ERROR", "Internal Server Error: " + ex.getMessage(), request));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(
            ConflictException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        buildError(
                                409,
                                "CONFLICT",
                                ex.getMessage(),
                                request
                        )
                );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        buildError(
                                401,
                                "UNAUTHORIZED",
                                "Tên đăng nhập hoặc mật khẩu không chính xác",
                                request
                        )
                );
    }

    private ErrorResponse buildError(
            Integer status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}
