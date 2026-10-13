package com.personalfinancetracker.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.personalfinancetracker.entity.EmailOtp;
import com.personalfinancetracker.entity.OtpPurpose;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailOtp> findTopByUser_IdAndPurposeOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailOtp> findTopByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            OtpPurpose purpose);

    long countByUser_IdAndPurposeAndCreatedAtAfter(Long userId, OtpPurpose purpose, Instant createdAfter);

    List<EmailOtp> findAllByUser_IdAndPurposeAndUsedAtIsNull(Long userId, OtpPurpose purpose);
}