package com.re.service.impl;

import com.re.exceptions.BadRequestException;
import com.re.exceptions.ForbiddenException;
import com.re.exceptions.ResourceNotFoundException;
import com.re.exceptions.ConflictException;
import com.re.model.dto.application.ApplicationRequest;
import com.re.model.entity.Application;
import com.re.model.entity.Job;
import com.re.model.entity.User;
import com.re.model.entity.enums.ApplicationStatus;
import com.re.repository.ApplicationRepository;
import com.re.repository.CompanyRepository;
import com.re.repository.JobRepository;
import com.re.repository.UserRepository;
import com.re.security.princical.CustomUserDetails;
import com.re.service.ApplicationService;
import com.re.service.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request) {

        // 1. Kiểm tra sự tồn tại của tin tuyển dụng (404 Not Found)
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + jobId));

        // 2. Kiểm tra sự tồn tại của ứng viên (404 Not Found)
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + candidateId));

        // 3. Kiểm tra xem ứng viên đã nộp hồ sơ vào vị trí này chưa (409 Conflict)
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new ConflictException("Bạn đã nộp hồ sơ vào vị trí này rồi!");
        }

        // 4. Kiểm tra định dạng file truyền lên, tránh NullPointerException (400 Bad Request)
        if (request.getCvUrl() == null || request.getCvUrl().isEmpty() || !"application/pdf".equals(request.getCvUrl().getContentType())) {
            throw new BadRequestException("Vui lòng chỉ tải lên tập tin định dạng PDF!");
        }

        // 5. Đẩy file lên Cloudinary lấy chuỗi URL bảo mật
        String uploadedCvUrl = cloudinaryService.uploadPdf(request.getCvUrl());

        // 6. Xây dựng đối tượng Application bằng Builder Pattern
        Application application = Application.builder()
                .coverLetter(request.getCoverLetter())
                .cvUrl(uploadedCvUrl)
                .status(ApplicationStatus.PENDING)
                .job(job)
                .feedback(null)
                .candidate(candidate)
                .build();

        return applicationRepository.save(application);
    }

    @Override
    @Transactional
    public Application updateStatus(Long applicationId, ApplicationStatus newStatus) {

        // 1. Lấy thông tin tài khoản đang đăng nhập trong Security Context
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long currentUserId = principal.getId();

        // 2. Tìm hồ sơ ứng tuyển thuộc quyền quản lý của nhà tuyển dụng này (403 Forbidden)
        Application application = applicationRepository.findApplicationForEmployer(applicationId, currentUserId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền thao tác trên hồ sơ này hoặc hồ sơ không tồn tại"));

        ApplicationStatus currentStatus = application.getStatus();

        // 3. Nếu trạng thái mới trùng với trạng thái cũ thì giữ nguyên không xử lý thêm
        if (currentStatus == newStatus) {
            return application;
        }

        // 4. Không cho phép sửa đổi khi hồ sơ đã đóng ở trạng thái cuối (ACCEPTED / REJECTED)
        if (currentStatus == ApplicationStatus.ACCEPTED || currentStatus == ApplicationStatus.REJECTED) {
            throw new ConflictException("Hồ sơ đã ở trạng thái cuối cùng, không thể thay đổi!");
        }

        // 5. Kiểm tra tính hợp lệ của State Machine (Luồng chuyển đổi trạng thái)
        switch (currentStatus) {
            case PENDING:
                if (newStatus != ApplicationStatus.REVIEWING && newStatus != ApplicationStatus.REJECTED) {
                    throw new ConflictException("Hồ sơ đang PENDING chỉ được chuyển sang REVIEWING hoặc REJECTED");
                }
                break;

            case REVIEWING:
                if (newStatus != ApplicationStatus.INTERVIEWING && newStatus != ApplicationStatus.REJECTED) {
                    throw new ConflictException("Hồ sơ đang REVIEWING chỉ được chuyển sang INTERVIEWING hoặc REJECTED");
                }
                break;

            case INTERVIEWING:
                if (newStatus != ApplicationStatus.ACCEPTED && newStatus != ApplicationStatus.REJECTED) {
                    throw new ConflictException("Hồ sơ đang INTERVIEWING chỉ được chuyển sang ACCEPTED hoặc REJECTED");
                }
                break;

            default:
                throw new ConflictException("Chuyển đổi trạng thái không hợp lệ!");
        }

        // 6. Cập nhật và lưu trạng thái mới
        application.setStatus(newStatus);
        return applicationRepository.save(application);
    }

    @Override
    public Application getApplicationByUser(Long userId, Long applicationId) {

        return applicationRepository.findApplicationForEmployer(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển phù hợp"));
    }
}