package com.re.service.impl;

import com.re.exceptions.ConflictException;
import com.re.exceptions.NoCompanyNameException;
import com.re.model.dto.auth.RegisterRequest;
import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.Company;
import com.re.model.entity.Role;
import com.re.model.entity.User;
import com.re.model.entity.enums.RegisterType;
import com.re.repository.CompanyRepository;
import com.re.repository.RoleRepository;
import com.re.repository.UserRepository;
import com.re.security.jwt.JWTProvider;
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        return mapping(user);
    }

    @Override
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã tồn tại");
        }

        if (request.getRegisterType() == RegisterType.EMPLOYER) {
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new NoCompanyNameException("Tài khoản nhà tuyển dụng bắt buộc phải nhập tên công ty!");
            }
        }


        Role role = roleRepository
                .findByRoleName(request.getRegisterType() == RegisterType.EMPLOYER ? "ROLE_EMPLOYER" : "ROLE_CANDIDATE")
                .orElseThrow(() -> new RuntimeException("Vai trò không tồn tại trên hệ thống"));


        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(false)
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

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().getRoleName())
                .build();
    }

    public UserResponse mapping(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null) // Check null an toàn cho Role
                .build();
    }
}