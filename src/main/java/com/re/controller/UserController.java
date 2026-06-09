package com.re.controller;

import com.re.model.dto.response.ApiDataResponse;
import com.re.model.dto.auth.UserResponse;
import com.re.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class UserController {
    private final UserServiceImpl userService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<Page<UserResponse>>> getUser(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Page<UserResponse> userAll = userService.getAllUser(pageable);

        return ResponseEntity.ok(
                new ApiDataResponse<>(true, "Get user success", userAll, null, HttpStatus.OK)
        );
    }
}
