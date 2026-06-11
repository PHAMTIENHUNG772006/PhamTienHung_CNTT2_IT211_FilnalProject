package com.re.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class ApplicationRequest {

    @NotBlank(message = "Cover letter không được để trống")
    private String coverLetter;

    @NotNull(message = "CV không được để trống")
    private MultipartFile cvUrl;
}
