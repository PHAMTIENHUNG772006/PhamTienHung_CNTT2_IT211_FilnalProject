package com.re.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

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

    @ExceptionHandler(NoCompanyNameException.class)
    public ResponseEntity<ErrorResponse> noCompanyName(
            NoCompanyNameException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        buildError(
                                400,
                                "Validation Error",
                                ex.getMessage(),
                                request
                        )
                );
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<?> handleAccountDisabledException(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "ACCOUNT_DISABLED",
                        "message", "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt!",
                        "path", "/api/v1/auth/login"
                ));
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        buildError(
                                403,
                                "FORBIDDEN",
                                ex.getMessage(),
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
