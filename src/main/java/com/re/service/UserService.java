package com.re.service;

import com.re.model.dto.auth.RegisterRequest;
import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUser(Pageable pageable);
    Page<UserResponse> findUserByFullName(String fullName, Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse lockUserAccount(Long userId);
    UserResponse createUser(RegisterRequest registerRequest);

}