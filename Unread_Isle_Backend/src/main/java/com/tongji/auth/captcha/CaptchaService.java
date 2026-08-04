package com.tongji.auth.captcha;

import com.tongji.auth.api.dto.CaptchaResponse;
import com.tongji.auth.config.AuthProperties;
import com.tongji.common.exception.BusinessException;
import com.tongji.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 生成并校验存储在 Redis 中的一次性图形验证码。 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String FIELD_ANSWER = "answer";
    private static final String FIELD_ATTEMPTS = "attempts";
    private static final String FIELD_MAX_ATTEMPTS = "maxAttempts";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            local answer = redis.call('HGET', KEYS[1], 'answer')
            if not answer then return 0 end
            local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0')
            local maxAttempts = tonumber(redis.call('HGET', KEYS[1], 'maxAttempts') or '5')
            if attempts >= maxAttempts then
                redis.call('DEL', KEYS[1])
                return -2
            end
            if answer == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            attempts = attempts + 1
            if attempts >= maxAttempts then
                redis.call('DEL', KEYS[1])
                return -2
            end
            redis.call('HSET', KEYS[1], 'attempts', attempts)
            return -1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties properties;

    public CaptchaResponse generate() {
        AuthProperties.Captcha config = properties.getCaptcha();
        String answer = randomAnswer(config.getCodeLength());
        String captchaId = UUID.randomUUID().toString();
        String key = buildKey(captchaId);

        HashOperations<String, String, String> hash = redisTemplate.opsForHash();
        hash.put(key, FIELD_ANSWER, answer);
        hash.put(key, FIELD_ATTEMPTS, "0");
        hash.put(key, FIELD_MAX_ATTEMPTS, String.valueOf(config.getMaxAttempts()));
        redisTemplate.expire(key, config.getTtl());

        return new CaptchaResponse(
                captchaId,
                renderImage(answer, config.getWidth(), config.getHeight()),
                config.getTtl().toSeconds()
        );
    }

    public void verify(String captchaId, String submittedAnswer) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(submittedAnswer)) {
            throw new BusinessException(ErrorCode.CAPTCHA_MISMATCH);
        }

        String key = buildKey(captchaId.trim());
        String actual = submittedAnswer.trim().toUpperCase(Locale.ROOT);
        Long result = redisTemplate.execute(VERIFY_SCRIPT, List.of(key), actual);
        if (result == null || result == 0L) {
            throw new BusinessException(ErrorCode.CAPTCHA_NOT_FOUND);
        }
        if (result == -2L) {
            throw new BusinessException(ErrorCode.CAPTCHA_TOO_MANY_ATTEMPTS);
        }
        if (result == 1L) {
            return;
        }
        throw new BusinessException(ErrorCode.CAPTCHA_MISMATCH);
    }

    private static String randomAnswer(int length) {
        int safeLength = Math.max(4, Math.min(length, 6));
        StringBuilder answer = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            answer.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return answer.toString();
    }

    private static String renderImage(String answer, int requestedWidth, int requestedHeight) {
        int width = Math.max(requestedWidth, 100);
        int height = Math.max(requestedHeight, 40);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(255, 249, 240));
            graphics.fillRect(0, 0, width, height);

            for (int i = 0; i < 8; i++) {
                graphics.setColor(randomColor(150, 220));
                graphics.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                        RANDOM.nextInt(width), RANDOM.nextInt(height));
            }

            int fontSize = Math.max(24, height - 15);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics(font);
            int cellWidth = width / answer.length();
            for (int i = 0; i < answer.length(); i++) {
                String character = String.valueOf(answer.charAt(i));
                int x = i * cellWidth + Math.max(3, (cellWidth - metrics.stringWidth(character)) / 2);
                int y = (height - metrics.getHeight()) / 2 + metrics.getAscent() + RANDOM.nextInt(5) - 2;
                double angle = Math.toRadians(RANDOM.nextInt(25) - 12);
                AffineTransform original = graphics.getTransform();
                graphics.rotate(angle, x + cellWidth / 2.0, height / 2.0);
                graphics.setColor(randomColor(45, 125));
                graphics.drawString(character, x, y);
                graphics.setTransform(original);
            }

            for (int i = 0; i < width / 5; i++) {
                graphics.setColor(randomColor(120, 210));
                graphics.fillOval(RANDOM.nextInt(width), RANDOM.nextInt(height), 2, 2);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render captcha image", ex);
        }
    }

    private static Color randomColor(int min, int max) {
        int range = max - min + 1;
        return new Color(
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range)
        );
    }

    private static String buildKey(String captchaId) {
        return "auth:captcha:" + captchaId;
    }
}
