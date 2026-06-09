package com.re.service.impl;

import com.re.exceptions.NotFoundJobException;
import com.re.model.dto.jobs.JobRequest;
import com.re.model.entity.Company;
import com.re.model.entity.Job;
import com.re.model.entity.User;
import com.re.repository.CompanyRepository;
import com.re.repository.JobRepository;
import com.re.repository.UserRepository;
import com.re.service.JobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;


    @Override
    public Job getJobById(Long id) {

        return jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundJobException("Không tìm thấy công việc cần tìm"));
    }

    @Override
    @Transactional
    public Job createJob(JobRequest jobRequest) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();


        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản đang đăng nhập: " + currentUsername));


        Company company = companyRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Tài khoản doanh nghiệp của bạn chưa được liên kết với hồ sơ công ty nào!"));


        Job job = Job.builder()
                .title(jobRequest.getTitle())
                .description(jobRequest.getDescription())
                .salaryRange(jobRequest.getSalaryRange())
                .company(company)
                .build();

        return jobRepository.save(job);
    }
}
