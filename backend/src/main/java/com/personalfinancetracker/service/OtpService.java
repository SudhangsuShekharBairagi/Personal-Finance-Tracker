package com.personalfinancetracker.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.EmailOtp;
import com.personalfinancetracker.entity.OtpPurpose;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.EmailOtpRepository;

@Service
public class OtpService {
    private final EmailOtpRepository otpRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration expiration;
    private final Duration resendCooldown;
    private final int maximumAttempts;
    private final int maximumPerHour;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            EmailOtpRepository otpRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${app.auth.otp.expiration:10m}") Duration expiration,
            @Value("${app.auth.otp.resend-cooldown:1m}") Duration resendCooldown,
            @Value("${app.auth.otp.max-attempts:5}") int maximumAttempts,
            @Value("${app.auth.otp.max-per-hour:5}") int maximumPerHour) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.expiration = expiration;
        this.resendCooldown = resendCooldown;
        this.maximumAttempts = maximumAttempts;
        this.maximumPerHour = maximumPerHour;
    }

    @Transactional
    public String issue(AppUser user, OtpPurpose purpose) {
        AppUser lockedUser = userRepository.findByIdForOtpUpdate(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "OTP_UNAVAILABLE",
                        "A verification code is unavailable."));
        Instant now = clock.instant();
        long sentThisHour = otpRepository.countByUser_IdAndPurposeAndCreatedAtAfter(
                lockedUser.getId(), purpose, now.minus(Duration.ofHours(1)));
        if (sentThisHour >= maximumPerHour) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED",
                    "Please wait before requesting another code.");
        }

        var mostRecent = otpRepository.findTopByUser_IdAndPurposeOrderByCreatedAtDesc(lockedUser.getId(), purpose);
        if (mostRecent.isPresent() && mostRecent.get().getCreatedAt().plus(resendCooldown).isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED",
                    "Please wait before requesting another code.");
        }

        otpRepository.findAllByUser_IdAndPurposeAndUsedAtIsNull(lockedUser.getId(), purpose)
                .forEach(otp -> otp.markUsed(now));

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        otpRepository.save(new EmailOtp(lockedUser, purpose, passwordEncoder.encode(code), now.plus(expiration)));
        return code;
    }

    @Transactional
    public boolean verify(AppUser user, OtpPurpose purpose, String submittedCode) {
        Instant now = clock.instant();
        var optionalOtp = otpRepository.findTopByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                user.getId(), purpose);
        if (optionalOtp.isEmpty()) {
            return false;
        }

        EmailOtp otp = optionalOtp.get();
        if (!otp.getExpiresAt().isAfter(now) || otp.getAttempts() >= maximumAttempts) {
            otp.markUsed(now);
            return false;
        }

        if (!passwordEncoder.matches(submittedCode, otp.getCodeHash())) {
            otp.recordFailedAttempt();
            if (otp.getAttempts() >= maximumAttempts) {
                otp.markUsed(now);
            }
            return false;
        }

        otp.markUsed(now);
        return true;
    }

    public int getExpirationMinutes() {
        return Math.max(1, (int) Math.ceil(expiration.toSeconds() / 60.0));
    }
}