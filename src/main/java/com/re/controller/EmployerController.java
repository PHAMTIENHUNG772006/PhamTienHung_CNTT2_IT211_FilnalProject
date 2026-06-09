package com.re.controller;

import com.re.model.dto.jobs.JobRequest;
import com.re.model.dto.response.ApiDataResponse;
import com.re.model.entity.Job;
import com.re.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final JobService jobService;

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
}