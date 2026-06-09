package com.re.service.impl;

import com.re.model.dto.application.ApplicationRequest;
import com.re.model.entity.Application;
import com.re.model.entity.Job;
import com.re.model.entity.User;
import com.re.model.entity.enums.ApplicationStatus;
import com.re.repository.ApplicationRepository;
import com.re.repository.CompanyRepository;
import com.re.repository.JobRepository;
import com.re.repository.UserRepository;
import com.re.service.ApplicationService;
import com.re.service.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;


    @Transactional
    public Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request) {

        // 1. Kiểm tra Job và Candidate xem có tồn tại không
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng này"));
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // 2. Kiểm tra xem ứng viên đã nộp đơn vào Job này chưa
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new RuntimeException("Bạn đã nộp hồ sơ vào vị trí này rồi!");
        }

        // 3. Kiểm tra định dạng file (Bắt buộc phải là PDF)
        if (request.getCvUrl().isEmpty() || !request.getCvUrl().getContentType().equals("application/pdf")) {
            throw new RuntimeException("Vui lòng chỉ upload file định dạng PDF!");
        }

        // 4. Tiến hành đẩy file lên Cloudinary lấy chuỗi URL
        String uploadedCvUrl = cloudinaryService.uploadPdf(request.getCvUrl());

        // 5. Build thực thể Application với thông tin đầy đủ và lưu vào DB
        Application application = Application.builder()
                .coverLetter(request.getCoverLetter())
                .cvUrl(uploadedCvUrl)
                .status(ApplicationStatus.PENDING)
                .job(job)
                .candidate(candidate)
                .build();

        return applicationRepository.save(application);
    }

    @Override
    public Application updateJob(Application application) {
        return null;
    }
}
