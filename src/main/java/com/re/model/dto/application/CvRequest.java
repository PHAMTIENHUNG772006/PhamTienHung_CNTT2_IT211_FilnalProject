package com.re.model.dto.application;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class CvRequest {
    @NotNull(message = "Vui lòng chọn file CV")
    private MultipartFile cvFile;
}