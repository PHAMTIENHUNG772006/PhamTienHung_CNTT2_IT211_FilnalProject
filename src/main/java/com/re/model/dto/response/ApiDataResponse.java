package com.re.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApiDataResponse <T>{
    private Boolean success;
    private String message;
    private T data;
    private Object error;
    private HttpStatus status;
}
