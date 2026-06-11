package com.re.service;

import com.re.model.dto.jobs.JobRequest; // Import DTO nhận dữ liệu từ Client
import com.re.model.entity.Job;
import com.re.model.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface JobService {


    Job getJobById(Long id);

    List<Job> findJobsByTitle(String title);

    Job createJob(JobRequest jobRequest);

    Job updateStatus(Long id, JobStatus jobStatus);

    Job closeJob(Long id);

    Page<Job> getAllJobsByEmployer(Long employerId, Pageable pageable);

    Page<Job> getAllJobsForCandidate(Pageable pageable);

}