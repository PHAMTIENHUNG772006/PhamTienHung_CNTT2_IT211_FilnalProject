package com.re.service.impl;

import com.re.exceptions.ConflictException;
import com.re.exceptions.NoCompanyNameException;
import com.re.exceptions.NotFountUserException;
import com.re.model.dto.auth.AuthResponse;
import com.re.model.dto.auth.UserResponse;
import com.re.model.dto.auth.LoginRequest;
import com.re.model.dto.auth.RefreshTokenRequest;
import com.re.model.dto.auth.RegisterRequest;
import com.re.model.entity.*;
import com.re.model.entity.enums.RegisterType;
import com.re.repository.*;
import com.re.security.jwt.JWTProvider;
import com.re.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final CompanyRepository companyRepository;

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

        if (user.getIsActive() == true) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        refreshTokenRepository.deleteByUser(user);

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

    // UC-03 & FR-03: Đăng xuất hệ thống - Xóa phiên làm việc Refresh Token và đưa Access Token hiện hành vào Blacklist
    @Override
    @Transactional
    public void logout(String accessToken, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFountUserException("Không tìm thấy người dùng: " + username));

        refreshTokenRepository.deleteByUser(user);

        // Đưa access token vào blacklist
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            String pureToken = accessToken.substring(7);
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30);

            TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                    .tokenString(pureToken)
                    .expiryTime(expiryTime)
                    .build();

            tokenBlacklistRepository.save(blacklistEntry);
        }
    }
}