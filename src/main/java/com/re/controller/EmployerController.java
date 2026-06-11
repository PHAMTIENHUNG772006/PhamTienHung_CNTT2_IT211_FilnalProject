package com.re.controller;

import com.re.model.dto.jobs.JobRequest;
import com.re.model.dto.response.ApiDataResponse;
import com.re.model.entity.Application;
import com.re.model.entity.Job;
import com.re.model.entity.enums.JobStatus;
import com.re.service.ApplicationService;
import com.re.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final JobService jobService;
    private final ApplicationService applicationService;

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

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiDataResponse<Job>> updateJobStatus(
            @PathVariable Long id,
            @RequestParam JobStatus status) {

        Job updatedJob = jobService.updateStatus(id, status);

        ApiDataResponse<Job> response = new ApiDataResponse<>(
                true,
                "Tạo bài đăng tuyển dụng thành công!",
                updatedJob,
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