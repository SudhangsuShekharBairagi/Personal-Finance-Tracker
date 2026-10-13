package com.personalfinancetracker.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.personalfinancetracker.dto.ApiError;

public class ExpiredSessionGuardFilter extends OncePerRequestFilter {
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ExpiredSessionGuardFilter(SessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var session = request.getSession(false);
        if (authentication != null && authentication.isAuthenticated() && session != null) {
            SessionInformation sessionInformation = sessionRegistry.getSessionInformation(session.getId());
            if (sessionInformation != null && sessionInformation.isExpired()) {
                logoutHandler.logout(request, response, authentication);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                objectMapper.writeValue(response.getOutputStream(), new ApiError(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        "SESSION_EXPIRED",
                        "Your session has expired. Sign in again.",
                        Map.of()));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}