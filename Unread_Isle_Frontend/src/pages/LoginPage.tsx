import { FormEvent, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import type { LoginRequest } from "@/types/auth";
import { authService } from "@/services/authService";
import { useCaptcha } from "@/features/auth/useCaptcha";
import styles from "./LoginPage.module.css";

type LocationState = {
  from?: string;
};

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isLoading, user } = useAuth();
  const [identifier, setIdentifier] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [message, setMessage] = useState<string | null>(null);
  const {
    captchaId,
    captchaImage,
    captchaAnswer,
    setCaptchaAnswer,
    captchaLoading,
    captchaError,
    refreshCaptcha
  } = useCaptcha();

  const from = (location.state as LocationState | undefined)?.from ?? "/";

  useEffect(() => {
    if (!isLoading && user) {
      navigate(from, { replace: true });
    }
  }, [isLoading, user, navigate, from]);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = window.setTimeout(() => setCountdown(prev => prev - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [countdown]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const payload: LoginRequest = { identifierType: "EMAIL", identifier, code };
      await login(payload);
      navigate(from, { replace: true });
    } catch (err) {
      const message = err instanceof Error ? err.message : "登录失败，请稍后重试";
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSendCode = async () => {
    if (!identifier) {
      setError("请先填写邮箱地址");
      return;
    }
    if (!captchaId || !captchaAnswer) {
      setError("请先填写图形验证码");
      return;
    }
    setError(null);
    setMessage(null);
    setSendingCode(true);
    try {
      const response = await authService.sendCode({
        scene: "LOGIN",
        identifierType: "EMAIL",
        identifier,
        captchaId,
        captchaAnswer
      });
      setCountdown(60);
      setMessage(`邮件验证码已发送，${Math.ceil(response.expireSeconds / 60)} 分钟内有效`);
    } catch (err) {
      const info = err instanceof Error ? err.message : "验证码发送失败";
      setError(info);
    } finally {
      setSendingCode(false);
      void refreshCaptcha().catch(() => undefined);
    }
  };

  const isDisabled = submitting || !identifier || !code;

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.titleBlock}>
          <h1 className={styles.title}>欢迎回来</h1>
          <p className={styles.subtitle}>登录未读岛，发现你的下一页</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          {/* 邮箱 + 图形验证码 + 邮件验证码登录 */}

          <div className={styles.field}>
            <label className={styles.label} htmlFor="identifier">
              邮箱
            </label>
            <input
              id="identifier"
              className={styles.input}
              value={identifier}
              onChange={event => setIdentifier(event.target.value)}
              placeholder="请输入邮箱地址"
              type="email"
              autoComplete="email"
            />
          </div>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="captcha">图形验证码</label>
            <div className={styles.captchaRow}>
              <input
                id="captcha"
                className={styles.input}
                value={captchaAnswer}
                onChange={event => setCaptchaAnswer(event.target.value.toUpperCase())}
                placeholder="请输入图中字符"
                autoComplete="off"
                maxLength={6}
              />
              <button
                type="button"
                className={styles.captchaButton}
                onClick={() => void refreshCaptcha().catch(() => undefined)}
                disabled={captchaLoading}
                aria-label="换一张图形验证码"
                title="点击换一张"
              >
                {captchaImage
                  ? <img src={captchaImage} alt="图形验证码，点击更换" />
                  : <span>{captchaLoading ? "加载中" : "点击刷新"}</span>}
              </button>
            </div>
            <button
              type="button"
              className={styles.refreshCaptcha}
              onClick={() => void refreshCaptcha().catch(() => undefined)}
              disabled={captchaLoading}
            >
              看不清？换一张
            </button>
            {captchaError ? <span className={styles.captchaError}>{captchaError}</span> : null}
          </div>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="code">
              邮件验证码
            </label>
            <div className={styles.codeRow}>
              <input
                id="code"
                className={styles.input}
                value={code}
                onChange={event => setCode(event.target.value)}
                placeholder="请输入邮件验证码"
                autoComplete="one-time-code"
              />
              <button
                type="button"
                className={styles.codeButton}
                disabled={sendingCode || countdown > 0}
                onClick={handleSendCode}
              >
                {countdown > 0 ? `${countdown}s` : "获取验证码"}
              </button>
            </div>
            <span className={styles.tips}>邮件验证码用于校验登录，不需要输入密码。</span>
          </div>

          {error ? <div className={styles.error}>{error}</div> : null}
          {message ? <div className={styles.success}>{message}</div> : null}

          <div className={styles.actions}>
            <button type="submit" className={styles.submitButton} disabled={isDisabled}>
              {submitting ? "登录中..." : "登录"}
            </button>
            <div className={styles.switchLink}>
              还没有账号？
              <button
                type="button"
                style={{ background: "none", border: "none", color: "var(--color-primary-strong)", fontWeight: 600, cursor: "pointer" }}
                onClick={() => navigate("/register", { state: { from } })}
              >
                前往注册
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
