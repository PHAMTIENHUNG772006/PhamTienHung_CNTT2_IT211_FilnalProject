package com.re.service.impl;

import com.re.exceptions.BadRequestException;
import com.re.exceptions.ForbiddenException;
import com.re.exceptions.ResourceNotFoundException;
import com.re.exceptions.ConflictException;
import com.re.model.dto.application.ApplicationRequest;
import com.re.model.dto.application.ApplicationResponse;
import com.re.model.entity.Application;
import com.re.model.entity.Company;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    @Override
    @Transactional
    public Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request) {

        // =========================================================================
        // PHẦN 1: KIỂM TRA TỒN TẠI & RÀO CHẮN LOGIC (VALIDATION & SECURITY)
        // =========================================================================

        // Kiểm tra sự tồn tại của tin tuyển dụng
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + jobId));

        // Kiểm tra sự tồn tại của ứng viên
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + candidateId));


        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new ConflictException("Bạn đã nộp hồ sơ vào vị trí này rồi!");
        }


        // =========================================================================
        // PHẦN 2: XỬ LÝ QUYẾT ĐỊNH URL CV (CV LOGIC RESOLUTION)
        // =========================================================================

        String finalCvUrl = null;
        String userSavedCvUrl = candidate.getCvUrl();

        if (userSavedCvUrl != null && !userSavedCvUrl.trim().isEmpty()) {
            // Trường hợp 1: Tài khoản ĐÃ CÓ SẴN CV
            if (request.getCvUrl() != null && !request.getCvUrl().isEmpty()) {
                validatePdfFile(request.getCvUrl());
                finalCvUrl = cloudinaryService.uploadPdf(request.getCvUrl());
            } else {

                finalCvUrl = userSavedCvUrl;
            }
        } else {

            if (request.getCvUrl() == null || request.getCvUrl().isEmpty()) {
                throw new BadRequestException("Tài khoản của bạn chưa có CV sẵn, vui lòng tải lên file CV mới!");
            }

            validatePdfFile(request.getCvUrl());
            finalCvUrl = cloudinaryService.uploadPdf(request.getCvUrl());

            // TỰ ĐỘNG TỐI ƯU UX: Tiện tay cập nhật luôn CV mới này vào hồ sơ cá nhân để lần sau không cần chọn lại file
            candidate.setCvUrl(finalCvUrl);
            userRepository.save(candidate);
        }


        // =========================================================================
        // PHẦN 3: TẠO BẢN GHI ĐƠN ỨNG TUYỂN VÀ LƯU TRỮ (PERSISTENCE)
        // =========================================================================

        Application application = Application.builder()
                .coverLetter(request.getCoverLetter())
                .cvUrl(finalCvUrl)
                .status(ApplicationStatus.PENDING)
                .job(job)
                .feedback(null)
                .candidate(candidate)
                .build();

        return applicationRepository.save(application);
    }

    /**
     * Hàm kiểm tra cấu trúc/định dạng của file PDF đầu vào
     */
    private void validatePdfFile(MultipartFile file) {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new BadRequestException("Vui lòng chỉ tải lên tập tin định dạng PDF!");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Tên file không hợp lệ hoặc không phải đuôi .pdf");
        }
    }

    @Override
    @Transactional
    public ApplicationResponse updateStatus(
            Long applicationId,
            ApplicationStatus newStatus,
            String feedback
    ) {

        // Lấy user hiện tại
        CustomUserDetails principal =
                (CustomUserDetails) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long currentUserId = principal.getId();

        // Tìm hồ sơ
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy hồ sơ ứng tuyển"
                        )
                );

        // Kiểm tra chủ sở hữu job
        Long ownerId = Optional.ofNullable(application)
                .map(Application::getJob)
                .map(Job::getCompany)
                .map(Company::getOwner)
                .map(User::getId)
                .orElseThrow(() ->
                        new ConflictException(
                                "Không xác định được chủ sở hữu Job"
                        )
                );

        ApplicationStatus currentStatus = application.getStatus();

        // Không thay đổi trạng thái
        if (currentStatus == newStatus) {
            return mapToResponse(application);
        }

        if (currentStatus == ApplicationStatus.ACCEPTED) {

            throw new ConflictException(
                    "Hồ sơ đã ở trạng thái cuối cùng không thể hủy "
            );
        }

        // Trạng thái cuối
        if (currentStatus == ApplicationStatus.REJECTED) {

            throw new ConflictException(
                    "Hồ sơ đã ở trạng thái hủy, không thể cập nhật lại"
            );
        }

        // State Machine
        switch (currentStatus) {

            case PENDING:

                if (newStatus != ApplicationStatus.REVIEWING
                        && newStatus != ApplicationStatus.REJECTED) {

                    throw new ConflictException(
                            "PENDING chỉ được chuyển sang REVIEWING hoặc REJECTED"
                    );
                }
                break;

            case REVIEWING:

                if (newStatus != ApplicationStatus.INTERVIEWING
                        && newStatus != ApplicationStatus.REJECTED) {

                    throw new ConflictException(
                            "REVIEWING chỉ được chuyển sang INTERVIEWING hoặc REJECTED"
                    );
                }
                break;

            case INTERVIEWING:

                if (newStatus != ApplicationStatus.ACCEPTED
                        && newStatus != ApplicationStatus.REJECTED) {

                    throw new ConflictException(
                            "INTERVIEWING chỉ được chuyển sang ACCEPTED hoặc REJECTED"
                    );
                }
                break;

            default:
                throw new ConflictException(
                        "Chuyển trạng thái không hợp lệ"
                );
        }

        // Cập nhật trạng thái
        application.setStatus(newStatus);
        application.setFeedback(feedback);

        Application saved = applicationRepository.save(application);

        return mapToResponse(saved);
    }

    @Override
    public Application getApplicationByUser(Long userId, Long applicationId) {

        return applicationRepository.findApplicationForEmployer(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển phù hợp"));
    }

    private ApplicationResponse mapToResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .candidateName(application.getCandidate().getFullName())
                .jobTitle(application.getJob().getTitle())
                .status(application.getStatus())
                .feedback(application.getFeedback())
                .build();
    }
}