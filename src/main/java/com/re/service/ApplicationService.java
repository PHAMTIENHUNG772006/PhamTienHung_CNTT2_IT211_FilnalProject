package com.re.service;

import com.re.model.dto.application.ApplicationRequest;
import com.re.model.entity.Application;

public interface ApplicationService {
    Application submitApplicationWithFile(Long jobId, Long candidateId, ApplicationRequest request);
    Application updateJob(Application application);
}
