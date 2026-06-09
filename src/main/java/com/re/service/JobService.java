package com.re.service;

import com.re.model.dto.jobs.JobRequest; // Import DTO nhận dữ liệu từ Client
import com.re.model.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface JobService {

    // Đổi kiểu trả về từ Optional<Job> thành Job
    Job getJobById(Long id);

    // 3. Tạo mới công việc (Sử dụng JobRequest DTO đầu vào)
    Job createJob(JobRequest jobRequest);

}