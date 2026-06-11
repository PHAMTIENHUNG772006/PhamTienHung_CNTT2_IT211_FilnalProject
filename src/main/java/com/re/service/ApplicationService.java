package com.re.service;

import com.re.model.dto.application.ApplicationRequest;
import com.re.model.dto.application.ApplicationResponse;
import com.re.model.entity.Application;
import com.re.model.entity.User;
import com.re.model.entity.enums.ApplicationStatus;

public interface ApplicationService {
    Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request);
    ApplicationResponse updateStatus(Long applicationId, ApplicationStatus status,String feedback);
    Application getApplicationByUser(Long userId, Long applicationId);
}
