package com.personalfinancetracker.security;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.personalfinancetracker.entity.AppUser;

public final class AppUserPrincipal implements UserDetails {
    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final boolean emailVerified;
    private final Instant createdAt;

    public AppUserPrincipal(AppUser user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.emailVerified = user.isEmailVerified();
        this.createdAt = user.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof AppUserPrincipal principal
                        && id != null
                        && id.equals(principal.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}