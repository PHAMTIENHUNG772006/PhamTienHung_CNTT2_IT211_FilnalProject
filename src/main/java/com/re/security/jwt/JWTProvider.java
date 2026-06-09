package com.re.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JWTProvider {

    @Value("${jwt-secret}")
    private String jwtSecret;

    @Value("${jwt-expired}")
    private Long jwtExpired;

    @Value("${jwt-refresh-expired}")
    private Long jwtRefreshExpired;

    /**
     * Sinh Access Token
     */
    public String generateAccessToken(String username) {
        return createToken(username, jwtExpired);
    }

    /**
     * Sinh Refresh Token
     */
    public String generateRefreshToken(String username) {
        return createToken(username, jwtRefreshExpired);
    }

    /**
     * Hàm dùng chung để tạo JWT
     */
    private String createToken(String username, Long expiredTime) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiredTime * 1000);

        SecretKey secretKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate JWT (Kiểm tra định dạng, chữ ký và thời hạn)
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    jwtSecret.getBytes(StandardCharsets.UTF_8)
            );

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (UnsupportedJwtException e) {
            log.error("JWT không được hỗ trợ");
            throw new RuntimeException("JWT không được hỗ trợ", e);
        } catch (ExpiredJwtException e) {
            log.error("JWT đã hết hạn");
            throw new RuntimeException("JWT đã hết hạn", e);
        } catch (MalformedJwtException e) {
            log.error("JWT sai định dạng");
            throw new RuntimeException("JWT sai định dạng", e);
        } catch (SignatureException e) {
            log.error("JWT sai chữ ký");
            throw new RuntimeException("JWT sai chữ ký", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT rỗng");
            throw new RuntimeException("JWT rỗng", e);
        } catch (JwtException e) {
            log.error("Lỗi xác thực JWT");
            throw new RuntimeException("Lỗi xác thực JWT", e);
        }
    }

    /**
     * Lấy Username từ JWT
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Lấy toàn bộ Claims từ Payload
     */
    public Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Kiểm tra token hết hạn chưa công khai
     */
    public boolean isExpired(String token) {
        return getExpirationDate(token).before(new Date());
    }

    /**
     * Lấy ngày hết hạn của Token
     */
    public Date getExpirationDate(String token) {
        return getClaims(token).getExpiration();
    }

    // =========================================================================
    // ĐOẠN ĐÃ ĐỒNG BỘ VÀ SỬA LỖI GẠCH ĐỎ CHO CÁC PHƯƠNG THỨC MỚI THÊM
    // =========================================================================

    /**
     * Kiểm tra ngày hết hạn (Dùng nội bộ)
     */
    private boolean isTokenExpired(String token) {
        return getExpirationDate(token).before(new Date());
    }

    /**
     * Trích xuất Username phục vụ cho Filter kiểm tra
     */
    public String extractUsername(String token) {
        try {
            return getUsernameFromToken(token);
        } catch (Exception e) {
            log.error("Không thể trích xuất username từ token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Xác thực Token có hợp lệ với Username hệ thống yêu cầu hay không
     */
    public boolean isTokenValid(String token, String username) {
        try {
            // Bước 1: Gọi hàm có sẵn cấu hình để check tính hợp lệ, chữ ký, định dạng cấu trúc
            validateToken(token);

            // Bước 2: Bóc username ra để so khớp
            final String extractedUsername = extractUsername(token);
            return (extractedUsername != null && extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Mã Token không hợp lệ hoặc đã hết hạn: {}", e.getMessage());
            return false;
        }
    }
}