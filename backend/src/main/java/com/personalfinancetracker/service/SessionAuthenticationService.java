package com.personalfinancetracker.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionAuthenticationService {
    private final SecurityContextRepository securityContextRepository;
    private final SessionRegistry sessionRegistry;

    public SessionAuthenticationService(
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry) {
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
    }

    public void establishSession(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (request.getSession(false) == null) {
            request.getSession(true);
        }
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        sessionRegistry.registerNewSession(request.getSession().getId(), authentication.getPrincipal());
    }
}