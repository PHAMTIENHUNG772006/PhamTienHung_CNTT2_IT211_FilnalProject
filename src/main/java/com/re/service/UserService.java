package com.re.service;

import com.re.model.dto.auth.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUser(Pageable pageable);
    Page<UserResponse> findUserByFullName(String fullName, Pageable pageable);
    UserResponse getUserById(Long id);
}