import { useCallback, useEffect, useState } from "react";
import { authService } from "@/services/authService";

export const useCaptcha = () => {
  const [captchaId, setCaptchaId] = useState("");
  const [captchaImage, setCaptchaImage] = useState("");
  const [captchaAnswer, setCaptchaAnswer] = useState("");
  const [captchaLoading, setCaptchaLoading] = useState(false);
  const [captchaError, setCaptchaError] = useState<string | null>(null);

  const refreshCaptcha = useCallback(async () => {
    setCaptchaLoading(true);
    setCaptchaError(null);
    try {
      const result = await authService.getCaptcha();
      setCaptchaId(result.captchaId);
      setCaptchaImage(result.imageData);
      setCaptchaAnswer("");
    } catch (error) {
      const message = error instanceof Error ? error.message : "图形验证码加载失败";
      setCaptchaError(message);
      throw error;
    } finally {
      setCaptchaLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshCaptcha().catch(() => undefined);
  }, [refreshCaptcha]);

  return {
    captchaId,
    captchaImage,
    captchaAnswer,
    setCaptchaAnswer,
    captchaLoading,
    captchaError,
    refreshCaptcha
  };
};
