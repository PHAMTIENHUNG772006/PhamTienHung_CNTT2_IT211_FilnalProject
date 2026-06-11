package com.re.service.impl;

import com.re.exceptions.BadRequestException;
import com.re.exceptions.ForbiddenException;
import com.re.exceptions.ResourceNotFoundException;
import com.re.exceptions.UnauthorizedException;
import com.re.exceptions.ConflictException;
import com.re.model.dto.auth.*;
import com.re.model.entity.*;
import com.re.model.entity.enums.RegisterType;
import com.re.repository.*;
import com.re.security.jwt.JWTProvider;
import com.re.service.AuthService;
import com.re.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JWTProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CompanyRepository companyRepository;
    private final RedisService redisService;

    // ========================= REGISTER =========================
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        // 1. Kiểm tra trùng lặp Email (409 Conflict)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã tồn tại");
        }

        // 2. Thay đổi sang BadRequestException (400 Bad Request) khi thiếu tên công ty đối với Employer
        if (request.getRegisterType() == RegisterType.EMPLOYER) {
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new BadRequestException("Tên công ty không được để trống");
            }
        }

        // 3. Thay đổi sang ResourceNotFoundException (404 Not Found) cho vai trò người dùng
        Role role = roleRepository.findByRoleName(
                        request.getRegisterType() == RegisterType.EMPLOYER
                                ? "ROLE_EMPLOYER"
                                : "ROLE_CANDIDATE")
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .role(role)
                .build();

        user = userRepository.save(user);

        if (request.getRegisterType() == RegisterType.EMPLOYER) {
            Company company = Company.builder()
                    .name(request.getCompanyName().trim())
                    .description("Chưa có mô tả công ty.")
                    .owner(user)
                    .build();

            companyRepository.save(company);
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().getRoleName())
                .build();
    }

    // ========================= LOGIN =========================
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 4. Sửa đổi sang ResourceNotFoundException (404 Not Found)
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // 5. Thay đổi sang ForbiddenException (403 Forbidden) cho tài khoản bị khóa
        if (!user.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }

        refreshTokenRepository.deleteByUserId(user.getId());

        String accessToken = jwtProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    // ========================= REFRESH TOKEN =========================
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        // 6. Thay đổi sang BadRequestException (400 Bad Request)
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token không được để trống");
        }

        jwtProvider.validateToken(refreshToken);

        // 7. Thay đổi sang ResourceNotFoundException (404 Not Found)
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token không tồn tại"));

        if (Boolean.TRUE.equals(tokenEntity.getRevoked())) {
            throw new ConflictException("Refresh token đã bị thu hồi");
        }

        // 8. Thay đổi sang UnauthorizedException (401 Unauthorized) khi token hết hạn
        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token đã hết hạn");
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);

        User user = tokenEntity.getUser();

        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        String newAccessToken = jwtProvider.generateAccessToken(username);
        String newRefreshToken = jwtProvider.generateRefreshToken(username);

        RefreshToken newToken = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(newToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .build();
    }

    // ========================= LOGOUT =========================
    @Override
    @Transactional
    public void logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        // 9. Thay đổi sang BadRequestException (400 Bad Request)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Token không hợp lệ");
        }

        String token = authHeader.substring(7);
        String username;

        try {
            username = jwtProvider.extractUsername(token);

            Date expiry = jwtProvider.getExpirationDateFromToken(token);
            long remainingTime = expiry.getTime() - System.currentTimeMillis();

            if (remainingTime > 0) {
                redisService.saveToBlacklist(token, remainingTime);
            }

        } catch (Exception e) {
            log.warn("Token invalid: {}", e.getMessage());
            username = null;
        }

        if (username == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            username = (auth != null) ? auth.getName() : null;
        }

        if (username != null) {
            // 10. Thay đổi sang ResourceNotFoundException (404 Not Found)
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

            refreshTokenRepository.deleteByUserId(user.getId());
        }
    }

    // ========================= FORGOT PASSWORD =========================
    @Override
    public String forgotPassword(String email) {

        // 11. Thay đổi sang ResourceNotFoundException (404 Not Found)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email chưa được đăng ký"));

        String otp = String.format("%06d", new Random().nextInt(999999));

        redisService.saveOtp(email, otp);

        log.info("OTP generated for {}: {}", email, otp);

        return otp;
    }

    // ========================= RESET PASSWORD =========================
    @Override
    public void resetPassword(ResetPasswordRequest request) {

        String savedOtp = redisService.getOtp(request.getEmail());

        // 12. Thay đổi sang BadRequestException (400 Bad Request) khi OTP hết hạn hoặc sai
        if (savedOtp == null) {
            throw new BadRequestException("OTP đã hết hạn hoặc không tồn tại");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new BadRequestException("OTP không chính xác");
        }

        // 13. Thay đổi sang ResourceNotFoundException (404 Not Found)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisService.deleteOtp(request.getEmail());
    }
}