package com.personalfinancetracker.repository;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.personalfinancetracker.entity.PasswordResetGrant;

public interface PasswordResetGrantRepository extends JpaRepository<PasswordResetGrant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetGrant> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            Instant now);
}