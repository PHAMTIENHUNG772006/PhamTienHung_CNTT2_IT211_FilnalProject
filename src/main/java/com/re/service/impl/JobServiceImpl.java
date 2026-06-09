package com.re.service.impl;

import com.re.exceptions.ConflictException;
import com.re.exceptions.NotFoundJobException;
import com.re.model.dto.jobs.JobRequest;
import com.re.model.entity.Company;
import com.re.model.entity.Job;
import com.re.model.entity.User;
import com.re.model.entity.enums.JobStatus;
import com.re.repository.CompanyRepository;
import com.re.repository.JobRepository;
import com.re.repository.UserRepository;
import com.re.service.JobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


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
    public List<Job> findJobsByTitle(String title) {

        List<Job> jobs = jobRepository.findAll().stream().filter(job -> job.getTitle().equals(title)).toList();

        return jobs;
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
                .status(JobStatus.DRAFT)
                .company(company)
                .applications(new ArrayList<>())
                .build();

        return jobRepository.save(job);
    }

    @Override
    @Transactional
    public Job updateStatus(Long id, JobStatus newStatus) {

        Job job = getJobById(id);
        JobStatus currentStatus = job.getStatus();

        // Điều kiện 1: Nếu trạng thái mới trùng với trạng thái cũ -> Không làm gì cả
        if (currentStatus == newStatus) {
            return job;
        }

        // Điều kiện 2: Một khi đã CLOSED thì không cho phép chỉnh sửa
        if (currentStatus == JobStatus.CLOSED) {
            throw new ConflictException("Công việc này đã đóng, không thể thay đổi trạng thái nữa.");
        }

        // Điều kiện 3: Kiểm tra tính hợp lệ của luồng di chuyển trạng thái (State Workflow)
        switch (currentStatus) {
            case DRAFT:

                if (newStatus != JobStatus.PENDING_APPROVAL) {
                    throw new ConflictException("Hành động không hợp lệ! Bài viết nháp chỉ có thể chuyển sang chờ phê duyệt.");
                }
                break;

            case PENDING_APPROVAL:
                if (newStatus != JobStatus.APPROVED && newStatus != JobStatus.REJECTED) {
                    throw new ConflictException("Hành động không hợp lệ! Bài đăng chờ duyệt chỉ có thể Chấp nhận hoặc Từ chối.");
                }
                break;

            case APPROVED:
                if (newStatus != JobStatus.CLOSED) {
                    throw new ConflictException("Hành động không hợp lệ! Bài đăng đang hoạt động chỉ có thể chuyển sang trạng thái Đóng.");
                }
                break;

            case REJECTED:
                if (newStatus != JobStatus.DRAFT) {
                    throw new ConflictException("Hành động không hợp lệ! Bài đăng bị từ chối phải quay về bản nháp để chỉnh sửa.");
                }
                break;
        }


        job.setStatus(newStatus);

        return jobRepository.save(job);
    }

    @Override
    public Page<Job> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }
}
