package com.tongji.auth.captcha;

import com.tongji.auth.api.dto.CaptchaResponse;
import com.tongji.auth.config.AuthProperties;
import com.tongji.common.exception.BusinessException;
import com.tongji.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, String, String> hashOperations;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        captchaService = new CaptchaService(redisTemplate, properties);
    }

    @Test
    void generateReturnsPngDataUrlAndIdentifier() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();

        CaptchaResponse response = captchaService.generate();

        assertNotNull(response.captchaId());
        assertTrue(response.imageData().startsWith("data:image/png;base64,"));
        assertEquals(120, response.expireSeconds());
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifyAcceptsAnswerIgnoringCaseAndWhitespace() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                eq("A7K9")
        )).thenReturn(1L);

        captchaService.verify("captcha-id", "  a7k9 ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifyRejectsExpiredCaptcha() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any()
        )).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> captchaService.verify("expired-id", "A7K9")
        );

        assertEquals(ErrorCode.CAPTCHA_NOT_FOUND, exception.getErrorCode());
    }
}
