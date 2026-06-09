package com.re.model.dto.jobs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Tiêu đề công việc không được để trống")
    @Size(max = 100, message = "Tiêu đề công việc không được vượt quá 100 ký tự")
    private String title;

    @NotBlank(message = "Mô tả công việc không được để trống")
    @Size(min = 10, message = "Mô tả công việc phải có ít nhất 10 ký tự")
    private String description;

    @NotBlank(message = "Khoảng lương không được để trống")
    @Size(max = 50, message = "Thông tin khoảng lương quá dài")
    private String salaryRange;
}