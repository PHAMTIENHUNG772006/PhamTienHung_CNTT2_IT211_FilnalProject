package com.re.service.impl;

import com.re.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lưu Token vào Blacklist
     * @param token Chuỗi token cần khóa
     * @param durationTime Thời gian còn lại của token (tính bằng mili-giây)
     */
    public void saveToBlacklist(String token, long durationTime) {
        redisTemplate.opsForValue().set(
                token,
                "blacklisted",
                durationTime,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Kiểm tra xem Token có nằm trong Blacklist hay không
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }

    // Lưu mã OTP vào Redis với thời gian sống là 5 phút
    public void saveOtp(String email, String otp) {
        redisTemplate.opsForValue().set("OTP_" + email, otp, 5, TimeUnit.MINUTES);
    }

    // Lấy mã OTP từ Redis ra để đối chiếu
    public String getOtp(String email) {
        return redisTemplate.opsForValue().get("OTP_" + email);
    }

    // Xóa OTP sau khi người dùng đã đổi mật khẩu thành công
    public void deleteOtp(String email) {
        redisTemplate.delete("OTP_" + email);
    }
}
