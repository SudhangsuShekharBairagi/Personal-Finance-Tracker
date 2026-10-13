import { useEffect, useState } from "react";
import api, { requestCsrfToken } from "../services/api.js";
import { AuthContext } from "./AuthContext.js";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");
  const [pendingEmail, setPendingEmail] = useState("");
  const [resetEmail, setResetEmail] = useState("");
  const [resetToken, setResetToken] = useState("");

  useEffect(() => {
    let active = true;

    api
      .get("/auth/me")
      .then(({ data }) => {
        if (!active) return;
        setUser(data);
        setStatus("authenticated");
      })
      .catch(() => {
        if (!active) return;
        setUser(null);
        setStatus("anonymous");
      });

    return () => {
      active = false;
    };
  }, []);

  async function postWithCsrf(path, body) {
    await requestCsrfToken();
    return api.post(path, body);
  }

  async function login(credentials) {
    const { data } = await postWithCsrf("/auth/login", credentials);
    setUser(data.user);
    setStatus("authenticated");
    return data.user;
  }

  async function register(details) {
    const { data } = await postWithCsrf("/auth/register", details);
    setPendingEmail(details.email.trim());
    return data;
  }

  async function verifyEmail(details) {
    const { data } = await postWithCsrf("/auth/verify-email", details);
    setPendingEmail(details.email.trim());
    return data;
  }

  async function resendVerification(email) {
    const { data } = await postWithCsrf("/auth/resend-verification", { email });
    return data;
  }

  async function requestPasswordReset(email) {
    const { data } = await postWithCsrf("/auth/forgot-password", { email });
    setResetEmail(email.trim());
    setResetToken("");
    return data;
  }

  async function verifyResetCode(details) {
    const { data } = await postWithCsrf("/auth/verify-reset-code", details);
    setResetEmail(details.email.trim());
    setResetToken(data.resetToken);
    return data;
  }

  async function resetPassword(newPassword) {
    if (!resetToken) {
      throw new Error(
        "Verify the password reset code before setting a new password.",
      );
    }
    const { data } = await postWithCsrf("/auth/reset-password", {
      resetToken,
      newPassword,
    });
    setResetToken("");
    setResetEmail("");
    return data;
  }

  async function logout() {
    await postWithCsrf("/auth/logout", {});
    setUser(null);
    setStatus("anonymous");
  }

  const value = {
    user,
    status,
    pendingEmail,
    setPendingEmail,
    resetEmail,
    resetToken,
    login,
    register,
    verifyEmail,
    resendVerification,
    requestPasswordReset,
    verifyResetCode,
    resetPassword,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
