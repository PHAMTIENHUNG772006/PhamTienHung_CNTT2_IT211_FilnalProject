package com.re.service.impl;

import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.User;
import com.re.repository.UserRepository;
import com.re.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Nhớ import thư viện này
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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