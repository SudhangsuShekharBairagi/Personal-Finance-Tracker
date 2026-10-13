package com.personalfinancetracker.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.dto.LoginRequest;
import com.personalfinancetracker.dto.RegisterRequest;
import com.personalfinancetracker.dto.ResetPasswordRequest;
import com.personalfinancetracker.dto.VerifyResetCodeResponse;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.OtpPurpose;
import com.personalfinancetracker.entity.PasswordResetGrant;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.exception.EmailDeliveryException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.PasswordResetGrantRepository;
import com.personalfinancetracker.security.AppUserPrincipal;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CODE_MESSAGE = "The code is invalid or expired.";
    private static final String RESET_ACCEPTED_MESSAGE = "If the account can be recovered, a reset message will be sent.";

    private final AppUserRepository userRepository;
    private final PasswordResetGrantRepository resetGrantRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final MailService mailService;
    private final SecureTokenGenerator tokenGenerator;
    private final AuthenticationManager authenticationManager;
    private final SessionRegistry sessionRegistry;
    private final Clock clock;
    private final Duration resetGrantExpiration;

    public AuthService(
            AppUserRepository userRepository,
            PasswordResetGrantRepository resetGrantRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            MailService mailService,
            SecureTokenGenerator tokenGenerator,
            AuthenticationManager authenticationManager,
            SessionRegistry sessionRegistry,
            Clock clock,
            @Value("${app.auth.reset-grant.expiration:15m}") Duration resetGrantExpiration) {
        this.userRepository = userRepository;
        this.resetGrantRepository = resetGrantRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.mailService = mailService;
        this.tokenGenerator = tokenGenerator;
        this.authenticationManager = authenticationManager;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
        this.resetGrantExpiration = resetGrantExpiration;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "REGISTRATION_UNAVAILABLE",
                    "Unable to register with those details.");
        }
        ensureBcryptCompatible(request.password());

        AppUser user = userRepository.saveAndFlush(new AppUser(
                request.name().strip(), email, passwordEncoder.encode(request.password())));
        String code = otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION);
        mailService.sendVerificationCode(email, user.getName(), code, otpService.getExpirationMinutes());
    }

    public AppUser verifyEmail(String email, String code) {
        AppUser user = findUser(email).orElseThrow(this::invalidOtp);
        if (user.isEmailVerified() || !otpService.verify(user, OtpPurpose.EMAIL_VERIFICATION, code)) {
            throw invalidOtp();
        }
        user.verifyEmail();
        return userRepository.save(user);
    }

    public void resendVerification(String email) {
        Optional<AppUser> optionalUser = findUser(email);
        if (optionalUser.isEmpty() || optionalUser.get().isEmailVerified()) {
            return;
        }

        AppUser user = optionalUser.get();
        try {
            String code = otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION);
            mailService.sendVerificationCode(user.getEmail(), user.getName(), code, otpService.getExpirationMinutes());
        } catch (ApiException exception) {
            if (exception.getStatus() != HttpStatus.TOO_MANY_REQUESTS)
                throw exception;
        } catch (EmailDeliveryException exception) {
            logger.warn("Verification email delivery failed; check SMTP configuration.");
        }
    }

    public Authentication login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (DisabledException exception) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Verify your email before signing in.");
        } catch (BadCredentialsException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
        }
    }

    public String forgotPassword(String email) {
        findUser(email)
                .filter(AppUser::isEmailVerified)
                .ifPresent(this::trySendPasswordResetCode);
        return RESET_ACCEPTED_MESSAGE;
    }

    public VerifyResetCodeResponse verifyResetCode(String email, String code) {
        AppUser user = findUser(email).orElseThrow(this::invalidOtp);
        if (!user.isEmailVerified() || !otpService.verify(user, OtpPurpose.PASSWORD_RESET, code)) {
            throw invalidOtp();
        }

        String resetToken = tokenGenerator.generate();
        Instant expiresAt = clock.instant().plus(resetGrantExpiration);
        resetGrantRepository.save(new PasswordResetGrant(
                user, tokenGenerator.sha256(resetToken), expiresAt));
        return new VerifyResetCodeResponse(resetToken, expiresAt);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        ensureBcryptCompatible(request.newPassword());
        Instant now = clock.instant();
        PasswordResetGrant grant = resetGrantRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        tokenGenerator.sha256(request.resetToken()), now)
                .orElseThrow(this::invalidResetGrant);

        grant.markUsed(now);
        AppUser user = grant.getUser();
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        resetGrantRepository.save(grant);
        userRepository.save(user);
        sessionRegistry.getAllSessions(new AppUserPrincipal(user), false)
                .forEach(session -> session.expireNow());
    }

    private void trySendPasswordResetCode(AppUser user) {
        try {
            String code = otpService.issue(user, OtpPurpose.PASSWORD_RESET);
            mailService.sendPasswordResetCode(user.getEmail(), user.getName(), code, otpService.getExpirationMinutes());
        } catch (ApiException exception) {
            if (exception.getStatus() != HttpStatus.TOO_MANY_REQUESTS)
                throw exception;
        } catch (EmailDeliveryException exception) {
            logger.warn("Password reset email delivery failed; check SMTP configuration.");
        }
    }

    private Optional<AppUser> findUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidOtp() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OR_EXPIRED_CODE", INVALID_CODE_MESSAGE);
    }

    private ApiException invalidResetGrant() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OR_EXPIRED_RESET",
                "The reset request is invalid or expired.");
    }

    private void ensureBcryptCompatible(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG",
                    "Password must be no more than 72 UTF-8 bytes.");
        }
    }
}