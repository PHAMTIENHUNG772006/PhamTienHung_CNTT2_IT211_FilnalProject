package com.re.controller;

import com.re.model.dto.application.ApplicationRequest;
import com.re.model.dto.response.ApiDataResponse;
import com.re.model.entity.Application;
import com.re.security.princical.CustomUserDetails;
import com.re.service.ApplicationService;
import com.re.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;
    private final ApplicationService applicationService;


    /**
     * API Nộp đơn kết hợp: Nhận cả chữ và file cùng một lúc
     * URL: POST http://localhost:8080/api/v1/applications/apply-with-file/{jobId}
     */
    @PostMapping("/apply-with-file/{jobId}")
    public ResponseEntity<?> applyJobWithFile(
            @PathVariable Long jobId,
            @Valid @ModelAttribute ApplicationRequest request) { // Dùng @ModelAttribute thay vì @RequestBody

        // Kiểm tra đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Bạn chưa đăng nhập hoặc phiên làm việc không hợp lệ");
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long candidateId = userDetails.getId();

            // Gọi Service thực hiện combo: Upload file -> Lưu DB
            Application result = applicationService.submitApplicationWithFile(jobId, candidateId, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Nộp hồ sơ ứng tuyển thành công! Mã đơn: " + result.getId());

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}