package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.EmailOtp;
import com.personalfinancetracker.entity.OtpPurpose;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.EmailOtpRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");

    @Mock
    private EmailOtpRepository otpRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OtpService otpService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(
                otpRepository,
                userRepository,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(10),
                Duration.ofMinutes(1),
                5,
                5);
        user = new AppUser("Demo User", "demo@example.test", "encoded-password");
    }

    @Test
    void issuesSixDigitOtpAsEncodedValue() {
        when(userRepository.findByIdForOtpUpdate(user.getId())).thenReturn(Optional.of(user));
        when(otpRepository.countByUser_IdAndPurposeAndCreatedAtAfter(
                eq(user.getId()), eq(OtpPurpose.EMAIL_VERIFICATION), any(Instant.class))).thenReturn(0L);
        when(otpRepository.findTopByUser_IdAndPurposeOrderByCreatedAtDesc(
                user.getId(), OtpPurpose.EMAIL_VERIFICATION)).thenReturn(Optional.empty());
        when(otpRepository.findAllByUser_IdAndPurposeAndUsedAtIsNull(
                user.getId(), OtpPurpose.EMAIL_VERIFICATION)).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));

        String code = otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION);

        assertTrue(code.matches("[0-9]{6}"));
        ArgumentCaptor<EmailOtp> saved = ArgumentCaptor.forClass(EmailOtp.class);
        verify(otpRepository).save(saved.capture());
        assertNotEquals(code, saved.getValue().getCodeHash());
        assertEquals("encoded:" + code, saved.getValue().getCodeHash());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), saved.getValue().getExpiresAt());
    }

    @Test
    void consumesMatchingOtpOnlyOnce() {
        EmailOtp otp = new EmailOtp(user, OtpPurpose.EMAIL_VERIFICATION, "encoded:123456", NOW.plusSeconds(600));
        when(otpRepository.findTopByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpPurpose.EMAIL_VERIFICATION)))
                .thenReturn(Optional.of(otp), Optional.empty());
        when(passwordEncoder.matches("123456", "encoded:123456")).thenReturn(true);

        assertTrue(otpService.verify(user, OtpPurpose.EMAIL_VERIFICATION, "123456"));
        assertFalse(otpService.verify(user, OtpPurpose.EMAIL_VERIFICATION, "123456"));
        assertNotNull(otp.getUsedAt());
    }

    @Test
    void expiresOtpAndLocksAfterFiveWrongAttempts() {
        EmailOtp expiredOtp = new EmailOtp(user, OtpPurpose.PASSWORD_RESET, "encoded:123456", NOW.minusSeconds(1));
        when(otpRepository.findTopByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpPurpose.PASSWORD_RESET))).thenReturn(Optional.of(expiredOtp));
        assertFalse(otpService.verify(user, OtpPurpose.PASSWORD_RESET, "123456"));
        assertNotNull(expiredOtp.getUsedAt());
        verify(passwordEncoder, never()).matches(anyString(), anyString());

        EmailOtp attemptedOtp = new EmailOtp(user, OtpPurpose.EMAIL_VERIFICATION, "encoded:123456",
                NOW.plusSeconds(600));
        when(otpRepository.findTopByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn(Optional.of(attemptedOtp));
        when(passwordEncoder.matches("000000", "encoded:123456")).thenReturn(false);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(otpService.verify(user, OtpPurpose.EMAIL_VERIFICATION, "000000"));
        }
        assertEquals(5, attemptedOtp.getAttempts());
        assertNotNull(attemptedOtp.getUsedAt());
    }

    @Test
    void enforcesHourlyCodeLimit() {
        when(userRepository.findByIdForOtpUpdate(user.getId())).thenReturn(Optional.of(user));
        when(otpRepository.countByUser_IdAndPurposeAndCreatedAtAfter(
                eq(user.getId()), eq(OtpPurpose.EMAIL_VERIFICATION), any(Instant.class))).thenReturn(5L);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        verify(otpRepository, never()).save(any(EmailOtp.class));
    }
}