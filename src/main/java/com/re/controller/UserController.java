package com.re.controller;

import com.re.model.dto.response.ApiDataResponse;
import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.Job;
import com.re.service.JobService;
import com.re.service.UserService;
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
    private final UserService userService;
    private final JobService  jobService;

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


    @GetMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiDataResponse<Page<Job>>> getAllJobs(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Page<Job> userAll = jobService.getAllJobs(pageable);

        return ResponseEntity.ok(
                new ApiDataResponse<>(true, "Lấy danh sách công việc có phân trang", userAll, null, HttpStatus.OK)
        );
    }
}
