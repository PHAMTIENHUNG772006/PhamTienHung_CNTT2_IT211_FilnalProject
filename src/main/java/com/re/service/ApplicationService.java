package com.re.service;

import com.re.model.dto.application.ApplicationRequest;
import com.re.model.entity.Application;
import com.re.model.entity.User;
import com.re.model.entity.enums.ApplicationStatus;

public interface ApplicationService {
    Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request);
    Application updateStatus(Long applicationId, ApplicationStatus status);
    Application getApplicationByUser(Long userId, Long applicationId);
}
