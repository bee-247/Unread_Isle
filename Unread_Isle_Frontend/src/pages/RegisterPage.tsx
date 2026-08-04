import { FormEvent, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { authService } from "@/services/authService";
import { useAuth } from "@/context/AuthContext";
import { useCaptcha } from "@/features/auth/useCaptcha";
import type { IdentifierType, RegisterRequest } from "@/types/auth";
import styles from "./RegisterPage.module.css";
// 注册方式固定为邮箱

const RegisterPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { register } = useAuth();
  const identifierType: IdentifierType = "EMAIL";
  const [identifier, setIdentifier] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const redirectTimerRef = useRef<number | null>(null);
  const {
    captchaId,
    captchaImage,
    captchaAnswer,
    setCaptchaAnswer,
    captchaLoading,
    captchaError,
    refreshCaptcha
  } = useCaptcha();

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = window.setTimeout(() => setCountdown(prev => prev - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [countdown]);

  useEffect(() => {
    return () => {
      if (redirectTimerRef.current) {
        window.clearTimeout(redirectTimerRef.current);
      }
    };
  }, []);

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
      await authService.sendCode({
        scene: "REGISTER",
        identifier,
        identifierType,
        captchaId,
        captchaAnswer
      });
      setMessage("邮件验证码已发送，请注意查收");
      setCountdown(60);
    } catch (err) {
      const info = err instanceof Error ? err.message : "验证码发送失败";
      setError(info);
    } finally {
      setSendingCode(false);
      void refreshCaptcha().catch(() => undefined);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      const payload: RegisterRequest = {
        identifierType,
        identifier,
        code,
        password,
        agreeTerms
      };
      await register(payload);
      setMessage("注册成功，已自动登录");
      // 直接跳回来源页面或首页
      const from = (location.state as { from?: string } | undefined)?.from ?? "/";
      redirectTimerRef.current = window.setTimeout(() => {
        navigate(from, { replace: true });
      }, 400);
    } catch (err) {
      const info = err instanceof Error ? err.message : "注册失败，请稍后重试";
      setError(info);
    } finally {
      setSubmitting(false);
    }
  };

  const isDisabled = submitting || !identifier || !code || !password || !agreeTerms;

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.titleBlock}>
          <h1 className={styles.title}>加入未读岛</h1>
          <p className={styles.subtitle}>完成注册，与更多人分享你的知识</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="identifier">邮箱</label>
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
            <span className={styles.tips}>邮件验证码用于验证邮箱所有权，有效期有限，请及时填写。</span>
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">
              登录密码
            </label>
            <input
              id="password"
              className={styles.input}
              type="password"
              value={password}
              onChange={event => setPassword(event.target.value)}
              placeholder="请设置不少于 8 位的密码"
              autoComplete="new-password"
            />
          </div>

          

          <div className={styles.field}>
            <div className={styles.checkboxRow}>
              <input
                id="agreeTerms"
                type="checkbox"
                checked={agreeTerms}
                onChange={event => setAgreeTerms(event.target.checked)}
              />
              <label className={styles.label} htmlFor="agreeTerms">
                我已阅读并同意
                <a href="#" onClick={e => e.preventDefault()}>《用户协议》</a>
                和
                <a href="#" onClick={e => e.preventDefault()}>《隐私政策》</a>
              </label>
            </div>
          </div>

          {error ? <div className={styles.error}>{error}</div> : null}
          {message ? <div className={styles.success}>{message}</div> : null}

          <div className={styles.actions}>
            <button type="submit" className={styles.submitButton} disabled={isDisabled}>
              {submitting ? "注册中..." : "立即注册"}
            </button>
            <div className={styles.switchLink}>
              已有账号？
              <button type="button" onClick={() => navigate("/login")}>返回登录</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterPage;
