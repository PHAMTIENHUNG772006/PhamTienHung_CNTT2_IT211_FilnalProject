package com.re.service.impl;

import com.re.exceptions.ConflictException;
import com.re.exceptions.NoCompanyNameException;
import com.re.exceptions.NotFountUserException;
import com.re.model.dto.auth.*;
import com.re.model.entity.*;
import com.re.model.entity.enums.RegisterType;
import com.re.repository.*;
import com.re.security.jwt.JWTProvider;
import com.re.security.princical.CustomUserDetails;
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

    // UC-02 & FR-04: Đăng ký tài khoản người dùng với Role Entity kết nối bảng dữ liệu khóa ngoại
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã tồn tại");
        }

        //  2. VALIDATE RIÊNG CHO EMPLOYER: Bắt buộc nhập tên công ty
        if (request.getRegisterType() == RegisterType.EMPLOYER) {
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new NoCompanyNameException("Tài khoản nhà tuyển dụng bắt buộc phải nhập tên công ty!");
            }
        }

        // 3. Tìm kiếm quyền cấu hình hệ thống
        Role role = roleRepository
                .findByRoleName(request.getRegisterType() == RegisterType.EMPLOYER ? "ROLE_EMPLOYER" : "ROLE_CANDIDATE")
                .orElseThrow(() -> new RuntimeException("Vai trò không tồn tại trên hệ thống"));

        // 4. Khởi tạo đối tượng User
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

        // 5. NẾU LÀ EMPLOYER: Tiến hành khởi tạo và lưu luôn thực thể Company mẫu
        if (request.getRegisterType() == RegisterType.EMPLOYER) {

            Company newCompany = Company.builder()
                    .name(request.getCompanyName().trim())
                    .description("Chưa có mô tả công ty.")
                    .owner(user)
                    .build();

            companyRepository.save(newCompany);
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

    // UC-01 & FR-01: Xác thực thông tin đăng nhập, xóa phiên token cũ và cấp phát cặp chuỗi JWT mới
    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new NotFountUserException("Không tìm thấy người dùng"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa");
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

    // FR-02: Cơ chế xoay vòng Token (Token Rotation) - Đổi chuỗi Refresh Token hợp lệ lấy cặp Token mới
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token không được để trống");
        }

        jwtProvider.validateToken(refreshToken);

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại"));

        if (Boolean.TRUE.equals(tokenEntity.getRevoked())) {
            throw new RuntimeException("Refresh token đã bị thu hồi");
        }

        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token đã hết hạn");
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);

        User user = tokenEntity.getUser();
        refreshTokenRepository.delete(tokenEntity);

        String newAccessToken = jwtProvider.generateAccessToken(username);
        String newRefreshToken = jwtProvider.generateRefreshToken(username);

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(newRefreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ hoặc không tồn tại");
        }

        String token = authHeader.substring(7);
        String username = null;

        // === PHẦN 1: ĐƯA ACCESS TOKEN VÀO REDIS BLACKLIST ===
        try {
            // Lấy username trực tiếp từ token để dùng cho phần 2
            username = jwtProvider.extractUsername(token);

            Date expirationDate = jwtProvider.getExpirationDateFromToken(token);
            long remainingTime = expirationDate.getTime() - System.currentTimeMillis();

            if (remainingTime > 0) {
                redisService.saveToBlacklist(token, remainingTime);
                log.info("Đã đưa Access Token của user [{}] vào Redis Blacklist", username);
            }
        } catch (Exception e) {
            log.warn("Token đã hết hạn hoặc không hợp lệ trước khi logout: {}", e.getMessage());
        }

        // === PHẦN 2: XÓA REFRESH TOKEN THEO USERNAME ===
        try {
            // Nếu không lấy được username từ token (do quá hạn), thử lấy từ SecurityContext
            if (username == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                    username = authentication.getName();
                }
            }

            if (username != null) {
                // Tìm User dựa trên tên đăng nhập
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new NotFountUserException("Không tìm thấy người dùng khi logout"));

                // Xóa toàn bộ phiên làm việc của user này
                refreshTokenRepository.deleteByUserId(user.getId());
                log.info("Đã xóa toàn bộ phiên làm việc Refresh Token của User: {}", username);
            } else {
                log.warn("Không thể xác định danh tính người dùng để xóa Refresh Token");
            }
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi xử lý xóa phiên Refresh Token: {}", e.getMessage());
        }
    }


    // Thay đổi từ void thành String để trả OTP trực tiếp ra ngoài
    @Override
    public String forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email này chưa được đăng ký trong hệ thống!"));

        // 2. Tạo mã OTP ngẫu nhiên gồm 6 chữ số
        String otp = String.format("%06d", new Random().nextInt(999999));

        redisService.saveOtp(email, otp);

        log.info("Khách hàng {} yêu cầu OTP khôi phục mật khẩu. Mã OTP sinh ra: {}", email, otp);

        return otp;
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Lấy OTP từ Redis ra kiểm tra
        String savedOtp = redisService.getOtp(request.getEmail());

        if (savedOtp == null) {
            throw new RuntimeException("Mã OTP đã hết hạn hoặc không tồn tại!");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP nhập vào không chính xác!");
        }

        // 2. OTP đúng -> Tìm User và cập nhật mật khẩu mới
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 3. Xóa OTP cũ trong Redis đi sau khi đổi thành công
        redisService.deleteOtp(request.getEmail());
    }
}