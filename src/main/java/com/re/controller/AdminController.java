package com.re.controller;

import com.re.model.dto.auth.RegisterRequest;
import com.re.model.dto.auth.UserResponse;
import com.re.model.dto.response.ApiDataResponse;
import com.re.model.entity.Job;
import com.re.model.entity.enums.JobStatus;
import com.re.service.JobService;
import com.re.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final JobService jobService;
    private final UserService userService;


    @PutMapping("/jobs/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<Job>> updateJobStatus(
            @PathVariable Long id,
            @RequestParam JobStatus status) {

        Job updatedJob = jobService.updateStatus(id, status);



        return new ResponseEntity<>(new ApiDataResponse<>(
                true,
                " Cập nhật trạng thái tin tuyển dụng",
                updatedJob,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }

    @PutMapping("/users/{id}/lockUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<UserResponse>> updateJobLock(@PathVariable("id") Long userId){

        UserResponse lockUser = userService.lockUserAccount(userId);

        return new ResponseEntity<>(new ApiDataResponse<>(
                true,
                "Khóa tài khoản người dùng",
                lockUser,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }


    @PostMapping("/createUser")
    public ResponseEntity<ApiDataResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse userResponse =
                userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiDataResponse<>(
                                true,
                                "Đăng ký thành công",
                                userResponse,
                                null,
                                HttpStatus.CREATED
                        )
                );
    }
}
