package com.personalfinancetracker.dto;

import java.time.Instant;

public record VerifyResetCodeResponse(String resetToken, Instant expiresAt) {
}