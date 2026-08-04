package com.tongji.auth.verification;

import com.tongji.auth.config.AuthProperties;
import com.tongji.common.exception.BusinessException;
import com.tongji.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 通过配置的 SMTP 服务发送邮箱验证码。 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.mail", name = "enabled", havingValue = "true")
public class EmailCodeSender implements CodeSender {

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    @Override
    public void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMail().getFrom());
        message.setTo(identifier);
        message.setSubject(subjectFor(scene));
        message.setText("你的验证码是：" + code + "\n\n验证码 " + expireMinutes
                + " 分钟内有效，请勿将验证码告诉他人。\n\n未读岛");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send verification email scene={} recipient={}", scene, maskEmail(identifier), ex);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private static String subjectFor(VerificationScene scene) {
        return switch (scene) {
            case REGISTER -> "未读岛注册验证码";
            case LOGIN -> "未读岛登录验证码";
            case RESET_PASSWORD -> "未读岛密码重置验证码";
        };
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
