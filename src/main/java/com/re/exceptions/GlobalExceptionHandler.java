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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========================= VALIDATION (DỮ LIỆU ĐẦU VÀO) =========================
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

    // ========================= SYSTEM & CUSTOM EXCEPTIONS (400 -> 404 -> 409) =========================

    // Hứng lỗi 400 - Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(400, "BAD_REQUEST", ex.getMessage(), request));
    }

    // Hứng lỗi 401 - Unauthorized (Token hết hạn, không hợp lệ)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(401, "UNAUTHORIZED", ex.getMessage(), request));
    }

    // Hứng lỗi 401 - Sai tài khoản hoặc mật khẩu từ Spring Security
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(401, "UNAUTHORIZED", "Tên đăng nhập hoặc mật khẩu không chính xác", request));
    }

    // Hứng lỗi 403 - Bị từ chối truy cập (Dùng Custom ForbiddenException của hệ thống)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(403, "FORBIDDEN", ex.getMessage(), request));
    }

    // Hứng lỗi 403 - Tài khoản bị khóa hoặc vô hiệu hóa từ Spring Security (Đã đồng bộ sang ErrorResponse)
    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleAccountDisabledException(Exception ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(403, "ACCOUNT_DISABLED", "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt!", request));
    }

    // Hứng lỗi 403 - Khi người dùng không đủ quyền hạn (Ví dụ: Candidate gọi API Admin)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(403, "FORBIDDEN", "Bạn không có quyền truy cập vào tài nguyên này!", request));
    }

    // Hứng lỗi 404 - Không tìm thấy tài nguyên dữ liệu
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildError(404, "NOT_FOUND", ex.getMessage(), request));
    }

    // Hứng lỗi 409 - Trùng lặp dữ liệu hoặc sai State Workflow
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(ConflictException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(409, "CONFLICT", ex.getMessage(), request));
    }

    // ========================= GLOBAL SYSTEM ERROR (500) =========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> global(Exception ex, HttpServletRequest request) {
        log.error("SYSTEM ERROR", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(500, "SERVER_ERROR", "Lỗi hệ thống nội bộ: " + ex.getMessage(), request));
    }

    // ========================= BUILD ERROR OBJECT =========================
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