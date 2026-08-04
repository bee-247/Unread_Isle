package com.tongji.auth.captcha;

import com.tongji.auth.api.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 图形验证码公开接口。 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public ResponseEntity<CaptchaResponse> captcha() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(captchaService.generate());
    }
}
