package com.re.controller;

import com.re.model.dto.application.ApplicationRequest;
import com.re.model.dto.response.ApiDataResponse;
import com.re.model.entity.Application;
import com.re.model.entity.Job;
import com.re.security.princical.CustomUserDetails;
import com.re.service.ApplicationService;
import com.re.service.CloudinaryService;
import com.re.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final JobService jobService;
    private final CloudinaryService cloudinaryService;
    private final ApplicationService applicationService;

    @GetMapping("/jobs")
    public ResponseEntity<ApiDataResponse<Page<Job>>> getAllJobs(
            @PageableDefault(page = 0, size = 2, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<Job> jobs = jobService.getAllJobsForCandidate(pageable);

        return ResponseEntity.ok(
                new ApiDataResponse<>(
                        true,
                        "Get jobs successfully",
                        jobs,
                        null,
                        HttpStatus.OK
                )
        );
    }


    /**
     * API Nộp đơn kết hợp: Nhận cả chữ và file cùng một lúc
     * URL: POST http://localhost:8080/api/v1/applications/apply-with-file/{jobId}
     */
    @PostMapping("/applications/apply-with-file/{jobId}")
    public ResponseEntity<ApiDataResponse<Application>> applyJobWithFile(
            @PathVariable Long jobId,
            @Valid @ModelAttribute ApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long candidateId = userDetails.getId();

        Application result = applicationService.submitApplicationWithFile(
                jobId,
                candidateId,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiDataResponse<>(
                        true,
                        "Apply công việc thành công",
                        result,
                        null,
                        HttpStatus.CREATED
                )
        );
    }
}