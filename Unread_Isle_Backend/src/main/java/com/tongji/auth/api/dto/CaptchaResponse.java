package com.tongji.auth.api.dto;

/** 新生成的图形验证码。 */
public record CaptchaResponse(
        String captchaId,
        String imageData,
        long expireSeconds
) {
}
