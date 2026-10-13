import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ArrowRight, Info, LoaderCircle } from "lucide-react";
import Button from "../../../components/Button.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import { useAuth } from "../../../hooks/useAuth.js";
import { getApiErrorMessage } from "../../../services/api.js";

const pageCopy = {
  login: {
    title: "Welcome back",
    detail: "Sign in to your account",
    submit: "Sign in",
  },
  register: {
    title: "Create an account",
    detail: "Get started with Pocketwise",
    submit: "Create account",
  },
  verify: {
    title: "Verify your email",
    detail: "One-time verification code",
    submit: "Verify email",
  },
  forgot: {
    title: "Reset your password",
    detail: "Password recovery",
    submit: "Send reset code",
  },
  reset: {
    title: "Choose a new password",
    detail: "Secure password reset",
    submit: "Verify reset code",
  },
};

function ConnectedAuthPage({ mode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    login,
    pendingEmail,
    register,
    requestPasswordReset,
    resendVerification,
    resetEmail,
    resetPassword,
    resetToken,
    setPendingEmail,
    user,
    verifyEmail,
    verifyResetCode,
  } = useAuth();
  const [email, setEmail] = useState(
    mode === "reset" ? resetEmail : pendingEmail,
  );
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(location.state?.message || "");
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const copy = pageCopy[mode];
  const enteringNewPassword = mode === "reset" && Boolean(resetToken);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setSubmitting(true);
    const formData = new FormData(event.currentTarget);
    const submittedEmail = String(formData.get("email") || email).trim();

    try {
      if (mode === "login") {
        await login({
          email: submittedEmail,
          password: String(formData.get("password")),
        });
        navigate(location.state?.from || "/dashboard", { replace: true });
        return;
      }

      if (mode === "register") {
        const name = String(formData.get("name")).trim();
        const password = String(formData.get("password"));
        if (password !== formData.get("confirmPassword")) {
          setError("The password entries do not match.");
          return;
        }
        setPendingEmail(submittedEmail);
        const response = await register({
          name,
          email: submittedEmail,
          password,
        });
        setSuccess(response.message);
        return;
      }

      if (mode === "verify") {
        await verifyEmail({
          email: submittedEmail,
          code: String(formData.get("code")),
        });
        setSuccess("Email verified. Sign in to continue.");
        return;
      }

      if (mode === "forgot") {
        const response = await requestPasswordReset(submittedEmail);
        setSuccess(response.message);
        return;
      }

      if (!enteringNewPassword) {
        await verifyResetCode({
          email: submittedEmail,
          code: String(formData.get("code")),
        });
        setEmail(submittedEmail);
        setSuccess("Code accepted. Choose a new password below.");
        return;
      }

      const newPassword = String(formData.get("newPassword"));
      if (newPassword !== formData.get("confirmPassword")) {
        setError("The password entries do not match.");
        return;
      }
      const response = await resetPassword(newPassword);
      navigate("/login", {
        replace: true,
        state: { message: response.message },
      });
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "The request could not be completed."),
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleResendVerification() {
    setError("");
    setSuccess("");
    if (!email || !email.includes("@")) {
      setError("Enter a valid email address before requesting another code.");
      return;
    }

    setResending(true);
    try {
      const response = await resendVerification(email.trim());
      setSuccess(response.message);
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "A verification code could not be sent.",
        ),
      );
    } finally {
      setResending(false);
    }
  }

  if (user && mode === "login") {
    return (
      <section className="w-full max-w-md">
        <h2 className="text-3xl font-semibold text-[#1b2a20]">
          You’re signed in
        </h2>
        <p className="mt-3 text-sm leading-6 text-[#68766c]">
          Signed in as {user.email}.
        </p>
        <Link className="mt-6 inline-flex" to="/dashboard">
          <Button icon={ArrowRight}>Continue to dashboard</Button>
        </Link>
      </section>
    );
  }

  return (
    <section className="w-full max-w-md">
      <p className="text-xs font-semibold uppercase tracking-[0.11em] text-emerald-800">
        {copy.detail}
      </p>
      <h2 className="mt-3 text-3xl font-semibold text-[#1b2a20]">
        {copy.title}
      </h2>
      <p className="mt-2 text-sm leading-6 text-[#68766c]">
        {/* Your form is sent to the configured backend. Verification and recovery
        codes require working SMTP settings. */}
      </p>

      {/* <div
        className="mt-6 flex items-start gap-2.5 rounded-md border border-[#dce8dd] bg-[#f2f7f2] px-3.5 py-3 text-xs leading-5 text-[#526258]"
        role="note"
      >
        <Info
          aria-hidden="true"
          className="mt-0.5 shrink-0 text-emerald-800"
          size={16}
        />
        <p>
          Passwords are sent only to the API. The browser does not save
          passwords or session tokens; the server manages an HTTP-only session
          cookie.
        </p>
      </div> */}

      {success && (
        <div
          className="mt-5 rounded-md border border-emerald-200 bg-emerald-50 px-3.5 py-3 text-sm leading-6 text-emerald-950"
          role="status"
        >
          {success}
        </div>
      )}
      {error && (
        <p className="mt-5 text-sm leading-6 text-rose-700" role="alert">
          {error}
        </p>
      )}

      <form className="mt-6 grid gap-4" onSubmit={handleSubmit}>
        {(mode === "login" ||
          mode === "register" ||
          mode === "verify" ||
          mode === "forgot" ||
          (mode === "reset" && !enteringNewPassword)) && (
          <Field htmlFor={`${mode}-email`} label="Email address">
            <input
              autoComplete="email"
              className={controlClassName}
              id={`${mode}-email`}
              name="email"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
              type="email"
              value={email}
            />
          </Field>
        )}

        {mode === "register" && (
          <Field htmlFor="register-name" label="Full name">
            <input
              autoComplete="name"
              className={controlClassName}
              id="register-name"
              maxLength={80}
              name="name"
              placeholder="Your name"
              required
            />
          </Field>
        )}
        {mode === "verify" && (
          <Field
            hint="The code expires and can only be used once."
            htmlFor="verify-code"
            label="Six-digit code"
          >
            <input
              autoComplete="one-time-code"
              className={controlClassName}
              id="verify-code"
              inputMode="numeric"
              maxLength={6}
              name="code"
              pattern="[0-9]{6}"
              placeholder="000000"
              required
            />
          </Field>
        )}
        {mode === "login" && (
          <Field htmlFor="login-password" label="Password">
            <input
              autoComplete="current-password"
              className={controlClassName}
              id="login-password"
              name="password"
              required
              type="password"
            />
          </Field>
        )}
        {mode === "register" && (
          <>
            <Field
              hint="Use at least 8 characters."
              htmlFor="register-password"
              label="Password"
            >
              <input
                autoComplete="new-password"
                className={controlClassName}
                id="register-password"
                maxLength={72}
                minLength={8}
                name="password"
                required
                type="password"
              />
            </Field>
            <Field htmlFor="register-confirm-password" label="Confirm password">
              <input
                autoComplete="new-password"
                className={controlClassName}
                id="register-confirm-password"
                maxLength={72}
                minLength={8}
                name="confirmPassword"
                required
                type="password"
              />
            </Field>
          </>
        )}
        {mode === "reset" && !enteringNewPassword && (
          <Field
            hint="This is the password reset code sent to your email."
            htmlFor="reset-code"
            label="Verification code"
          >
            <input
              autoComplete="one-time-code"
              className={controlClassName}
              id="reset-code"
              inputMode="numeric"
              maxLength={6}
              name="code"
              pattern="[0-9]{6}"
              placeholder="000000"
              required
            />
          </Field>
        )}
        {mode === "reset" && enteringNewPassword && (
          <>
            <p className="text-sm text-[#647168]">
              Reset code verified for {resetEmail}.
            </p>
            <Field
              hint="Use at least 8 characters."
              htmlFor="reset-new-password"
              label="New password"
            >
              <input
                autoComplete="new-password"
                className={controlClassName}
                id="reset-new-password"
                maxLength={72}
                minLength={8}
                name="newPassword"
                required
                type="password"
              />
            </Field>
            <Field
              htmlFor="reset-confirm-password"
              label="Confirm new password"
            >
              <input
                autoComplete="new-password"
                className={controlClassName}
                id="reset-confirm-password"
                maxLength={72}
                minLength={8}
                name="confirmPassword"
                required
                type="password"
              />
            </Field>
          </>
        )}

        <Button
          className="mt-1 w-full"
          disabled={submitting}
          icon={submitting ? LoaderCircle : ArrowRight}
          type="submit"
        >
          {submitting
            ? "Please wait…"
            : enteringNewPassword
              ? "Set new password"
              : copy.submit}
        </Button>
      </form>

      {mode === "verify" && (
        <Button
          className="mt-3 w-full"
          disabled={resending}
          onClick={handleResendVerification}
          variant="secondary"
        >
          {resending ? "Requesting code…" : "Resend verification code"}
        </Button>
      )}

      <div className="mt-6 flex flex-wrap justify-between gap-x-4 gap-y-2 text-sm">
        {(mode === "login" || mode === "register") && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/forgot-password"
          >
            Forgot password?
          </Link>
        )}
        {mode === "login" && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/verify-email"
          >
            Verify your email
          </Link>
        )}
        {mode === "register" && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/verify-email"
          >
            Enter verification code
          </Link>
        )}
        {mode === "forgot" && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/reset-password"
          >
            I have a reset code
          </Link>
        )}
        {mode === "verify" && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/login"
          >
            Back to sign in
          </Link>
        )}
        {(mode === "login" || mode === "forgot" || mode === "reset") && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/register"
          >
            Create account
          </Link>
        )}
        {mode === "register" && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/login"
          >
            Back to sign in
          </Link>
        )}
        {mode === "reset" && (
          <Link
            className="font-medium text-emerald-800 hover:text-emerald-950"
            to="/forgot-password"
          >
            Request another reset
          </Link>
        )}
      </div>
    </section>
  );
}

export default ConnectedAuthPage;
