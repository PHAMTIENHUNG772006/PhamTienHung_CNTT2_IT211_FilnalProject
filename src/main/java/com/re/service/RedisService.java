package com.re.service;

public interface RedisService {
    void saveToBlacklist(String token, long durationTime);
    void saveOtp(String email, String otp);
    void deleteOtp(String email);
    String getOtp(String email);
    boolean isTokenBlacklisted(String token);
}
