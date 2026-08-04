package com.tongji.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tongji.auth.model.IdentifierType;
import com.tongji.auth.verification.VerificationScene;

/**
 * 发送验证码请求。
 * <p>
 * `scene` 指定场景（注册/登录/重置密码）；发送邮件前必须携带并通过图形验证码。
 */
public record SendCodeRequest(
        @NotNull(message = "场景不能为空") VerificationScene scene,
        @NotNull(message = "账号类型不能为空") IdentifierType identifierType,
        @NotBlank(message = "账号不能为空") String identifier,
        @NotBlank(message = "图形验证码标识不能为空") String captchaId,
        @NotBlank(message = "请输入图形验证码") String captchaAnswer
) {
}
