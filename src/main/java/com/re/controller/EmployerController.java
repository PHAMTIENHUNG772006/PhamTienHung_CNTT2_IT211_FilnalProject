package com.re.controller;

import com.re.model.dto.application.ApplicationResponse;
import com.re.model.dto.jobs.JobRequest;
import com.re.model.dto.response.ApiDataResponse;
import com.re.model.entity.Application;
import com.re.model.entity.Job;
import com.re.model.entity.enums.ApplicationStatus;
import com.re.security.princical.CustomUserDetails;
import com.re.service.ApplicationService;
import com.re.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final JobService jobService;
    private final ApplicationService applicationService;


    @GetMapping("/jobs")
    public ResponseEntity<ApiDataResponse<Page<Job>>> getAllJobs(
            @PageableDefault(page = 0, size = 2, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long employerId = userDetails.getId();

        Page<Job> jobs = jobService.getAllJobsByEmployer(employerId, pageable);

        ApiDataResponse<Page<Job>> response = new ApiDataResponse<>(
                true,
                "Lấy danh sách job thành công",
                jobs,
                null,
                HttpStatus.OK
        );

        return ResponseEntity.ok(response);
    }

    // API tạo mới Bài tuyển dụng
    @PostMapping("/createJob")
    public ResponseEntity<ApiDataResponse<Job>> createJob(@Valid @RequestBody JobRequest jobRequest) {


        Job newJob = jobService.createJob(jobRequest);


        ApiDataResponse<Job> response = new ApiDataResponse<>(
                true,
                "Tạo bài đăng tuyển dụng thành công!",
                newJob,
                null,
                HttpStatus.CREATED
        );


        return ResponseEntity
                .created(URI.create("/api/v1/employer/job/" + newJob.getId()))
                .body(response);
    }


    @PutMapping("/{id}/closedJob")
    public ResponseEntity<ApiDataResponse<Job>> closeJob(@PathVariable Long id){

        Job job = jobService.closeJob(id);

        ApiDataResponse<Job> response = new ApiDataResponse<>(
                true,
                "Đóng bài thành công",
                job,
                null,
                HttpStatus.OK
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApiDataResponse<ApplicationResponse>> updateJobStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String message
    ) {

        ApplicationResponse application =
                applicationService.updateStatus(id, status,message);

        ApiDataResponse<ApplicationResponse> response =
                new ApiDataResponse<>(
                        true,
                        "Cập nhật trạng thái hồ sơ thành công!",
                        application,
                        null,
                        HttpStatus.OK
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/application/findUser")
    public ResponseEntity<ApiDataResponse<Application>> findUserApplication(@RequestParam Long userId, @RequestParam Long applicationId){

        Application application = applicationService.getApplicationByUser(userId, applicationId);

        ApiDataResponse<Application> response = new ApiDataResponse<>(
                true,
                "Tìm Kiếm thành công ứng viên!",
                application,
                null,
                HttpStatus.OK
        );

        return ResponseEntity.ok(response);
    }

}