package com.re.service.impl;

import com.re.exceptions.BadRequestException;
import com.re.exceptions.ResourceNotFoundException;
import com.re.exceptions.ConflictException;
import com.re.model.dto.auth.RegisterRequest;
import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.Company;
import com.re.model.entity.Role;
import com.re.model.entity.User;
import com.re.model.entity.enums.RegisterType;
import com.re.repository.CompanyRepository;
import com.re.repository.RoleRepository;
import com.re.repository.UserRepository;
import com.re.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserResponse> getAllUser(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(this::mapping);
    }

    @Override
    public UserResponse getUserById(Long id) {
        // 1. Thay RuntimeException bằng ResourceNotFoundException (404 Not Found)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapping(user);
    }

    @Override
    public Page<UserResponse> findUserByFullName(String fullName, Pageable pageable) {
        Page<User> userPage = userRepository.findByFullNameContaining(fullName, pageable);
        return userPage.map(this::mapping);
    }

    @Override
    @Transactional
    public UserResponse lockUserAccount(Long userId) {
        // 2. Thay RuntimeException bằng ResourceNotFoundException (404 Not Found)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Đảo trạng thái active (nếu true thành false, nếu false thành true)
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        return mapping(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        // 3. Kiểm tra trùng email (409 Conflict)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã tồn tại trên hệ thống!");
        }

        // 4. Thay NoCompanyNameException bằng BadRequestException (400 Bad Request)
        if (request.getRegisterType() == RegisterType.EMPLOYER) {
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new BadRequestException("Tài khoản nhà tuyển dụng bắt buộc phải nhập tên công ty!");
            }
        }

        // 5. Thay RuntimeException bằng ResourceNotFoundException (404 Not Found)
        Role role = roleRepository
                .findByRoleName(request.getRegisterType() == RegisterType.EMPLOYER ? "ROLE_EMPLOYER" : "ROLE_CANDIDATE")
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại trên hệ thống!"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true) // 💡 Tinh chỉnh: Cho phép tài khoản vừa tạo ở trạng thái active để có thể login ngay (trừ khi hệ thống của bạn có luồng kích hoạt mail riêng)
                .role(role)
                .build();

        user = userRepository.save(user);

        if (request.getRegisterType() == RegisterType.EMPLOYER) {
            Company newCompany = Company.builder()
                    .name(request.getCompanyName().trim())
                    .description("Chưa có mô tả công ty.")
                    .owner(user)
                    .build();

            companyRepository.save(newCompany);
        }

        return mapping(user); // 💡 Tối ưu: Sử dụng lại hàm mapping(user) có sẵn bên dưới thay vì viết lặp code Builder ở đây
    }

    // Hàm chuyển đổi từ Entity sang DTO
    public UserResponse mapping(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .build();
    }
}