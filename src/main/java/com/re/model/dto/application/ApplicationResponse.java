package com.re.model.dto.application;

import com.re.model.entity.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationResponse {

    private Long id;
    private String candidateName;
    private String jobTitle;
    private ApplicationStatus status;
    private String feedback;
}