package com.re.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class ApplicationRequest {
    private String coverLetter;
    private MultipartFile cvUrl;
}
