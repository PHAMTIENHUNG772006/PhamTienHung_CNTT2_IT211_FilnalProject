package com.re.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadPdf(MultipartFile file);
}
