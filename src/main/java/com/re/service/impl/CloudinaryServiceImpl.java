package com.re.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.re.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadPdf(MultipartFile file) {
        try {
            // Cấu hình các tham số upload
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", "candidate_cvs",
                    "resource_type", "auto"
            );

            // Tiến hành upload file lên Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

            // Trả về đường dẫn URL dạng https để lưu xuống Database (trường cvUrl của bạn)
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi xảy ra trong quá trình upload file PDF lên Cloudinary: " + e.getMessage());
        }
    }
}