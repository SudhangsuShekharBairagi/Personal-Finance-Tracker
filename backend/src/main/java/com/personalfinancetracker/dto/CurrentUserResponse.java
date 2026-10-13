package com.personalfinancetracker.dto;

import java.time.Instant;

import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.security.AppUserPrincipal;

public record CurrentUserResponse(Long id, String name, String email, Instant createdAt) {
    public static CurrentUserResponse from(AppUser user) {
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }

    public static CurrentUserResponse from(AppUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getId(), principal.getName(), principal.getEmail(), principal.getCreatedAt());
    }
}