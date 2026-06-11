package com.re.service.impl;

import com.re.exceptions.BadRequestException;
import com.re.exceptions.ForbiddenException;
import com.re.exceptions.ResourceNotFoundException;
import com.re.exceptions.ConflictException;
import com.re.model.dto.jobs.JobRequest;
import com.re.model.entity.Company;
import com.re.model.entity.Job;
import com.re.model.entity.User;
import com.re.model.entity.enums.JobStatus;
import com.re.repository.CompanyRepository;
import com.re.repository.JobRepository;
import com.re.repository.UserRepository;
import com.re.security.princical.CustomUserDetails;
import com.re.service.JobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    public Job getJobById(Long id) {
        // Thay NotFoundJobException bằng ResourceNotFoundException (404 Not Found)
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + id));
    }

    @Override
    public List<Job> findJobsByTitle(String title) {
        return jobRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    @Transactional
    public Job createJob(JobRequest jobRequest) {
        // Đồng bộ hóa cách lấy thông tin User từ SecurityContext qua CustomUserDetails
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long currentUserId = principal.getId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đang đăng nhập"));

        // Thay RuntimeException bằng BadRequestException (400 Bad Request)
        Company company = companyRepository.findByOwner(currentUser)
                .orElseThrow(() -> new BadRequestException("Tài khoản nhà tuyển dụng của bạn chưa được liên kết với hồ sơ công ty nào!"));

        Job job = Job.builder()
                .title(jobRequest.getTitle())
                .description(jobRequest.getDescription())
                .salaryRange(jobRequest.getSalaryRange())
                .status(JobStatus.PENDING_APPROVAL)
                .company(company)
                .applications(new ArrayList<>())
                .build();

        return jobRepository.save(job);
    }

    @Override
    @Transactional
    public Job updateStatus(Long id, JobStatus newStatus) {
        log.info("Cập nhật trạng thái cho tin tuyển dụng ID: {}", id);

        Job job = getJobById(id);
        JobStatus currentStatus = job.getStatus();

        // Điều kiện 1: Nếu trạng thái mới trùng với trạng thái cũ -> Không làm gì cả
        if (currentStatus == newStatus) {
            return job;
        }

        // Điều kiện 2: Một khi đã CLOSED thì không cho phép chỉnh sửa (409 Conflict)
        if (currentStatus == JobStatus.CLOSED) {
            throw new ConflictException("Tin tuyển dụng này đã đóng, không thể thay đổi trạng thái nữa.");
        }

        // Điều kiện 3: Kiểm tra tính hợp lệ của luồng di chuyển trạng thái (State Workflow)
        switch (currentStatus) {
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
                if (newStatus != JobStatus.PENDING_APPROVAL) {
                    throw new ConflictException("Hành động không hợp lệ! Bài đăng bị từ chối chỉ có thể yêu cầu phê duyệt lại.");
                }
                break;
        }

        job.setStatus(newStatus);
        return jobRepository.save(job);
    }

    @Override
    @Transactional
    public Job closeJob(Long jobId) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long currentUserId = principal.getId();

        // Thay AccessDeniedException bằng ForbiddenException (403 Forbidden)
        Job job = jobRepository.findJobForEmployer(jobId, currentUserId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền đóng tin tuyển dụng này hoặc tin này không tồn tại."));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin tuyển dụng này đã được đóng trước đó rồi.");
        }

        job.setStatus(JobStatus.CLOSED);
        return jobRepository.save(job);
    }

    @Override
    public Page<Job> getAllJobsByEmployer(Long employerId, Pageable pageable) {
        return jobRepository.findAllJobsByEmployer(employerId, pageable);
    }

    @Override
    public Page<Job> getAllJobsForCandidate(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }
}