package com.re.service.impl;

import com.re.exceptions.ConflictException;
import com.re.exceptions.NotFoundApplicationException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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


    @Transactional
    public Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request) {


        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng này"));
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));


        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new RuntimeException("Bạn đã nộp hồ sơ vào vị trí này rồi!");
        }

        // 3. Kiểm tra định dạng file (Bắt buộc phải là PDF)
        if (request.getCvUrl().isEmpty() || !request.getCvUrl().getContentType().equals("application/pdf")) {
            throw new RuntimeException("Vui lòng chỉ upload file định dạng PDF!");
        }

        // đẩy file lên Cloudinary lấy chuỗi URL
        String uploadedCvUrl = cloudinaryService.uploadPdf(request.getCvUrl());

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
    public Application updateStatus(
            Long applicationId,
            ApplicationStatus newStatus
    ) {

        CustomUserDetails principal =
                (CustomUserDetails)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        Long currentUserId = principal.getId();

        Application application =
                applicationRepository.findApplicationForEmployer(
                                applicationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Bạn không có quyền thao tác hồ sơ này hoặc hồ sơ không tồn tại"
                                )
                        );

        ApplicationStatus currentStatus =
                application.getStatus();

        if (currentStatus == newStatus) {
            return application;
        }

        if (
                currentStatus == ApplicationStatus.ACCEPTED
                        || currentStatus == ApplicationStatus.REJECTED
        ) {

            throw new ConflictException(
                    "Hồ sơ đã ở trạng thái cuối cùng, không thể thay đổi"
            );
        }

        switch (currentStatus) {

            case PENDING:

                if (
                        newStatus != ApplicationStatus.REVIEWING
                                && newStatus != ApplicationStatus.REJECTED
                ) {

                    throw new ConflictException(
                            "PENDING chỉ được chuyển sang REVIEWING hoặc REJECTED"
                    );
                }

                break;

            case REVIEWING:

                if (
                        newStatus != ApplicationStatus.INTERVIEWING
                                && newStatus != ApplicationStatus.REJECTED
                ) {

                    throw new ConflictException(
                            "REVIEWING chỉ được chuyển sang INTERVIEWING hoặc REJECTED"
                    );
                }

                break;

            case INTERVIEWING:

                if (
                        newStatus != ApplicationStatus.ACCEPTED
                                && newStatus != ApplicationStatus.REJECTED
                ) {

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

        application.setStatus(newStatus);

        return applicationRepository.save(application);
    }

    @Override
    public Application getApplicationByUser(Long userId, Long applicationId) {
        return applicationRepository.findApplicationForEmployer(userId,applicationId).orElseThrow(() -> new NotFoundApplicationException("Không có ứng viên nào được tìm thấy"));
    }
}
