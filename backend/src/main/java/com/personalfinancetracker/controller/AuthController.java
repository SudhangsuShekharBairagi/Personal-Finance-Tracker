package com.personalfinancetracker.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personalfinancetracker.dto.AuthResponse;
import com.personalfinancetracker.dto.CsrfInfo;
import com.personalfinancetracker.dto.CurrentUserResponse;
import com.personalfinancetracker.dto.ForgotPasswordRequest;
import com.personalfinancetracker.dto.LoginRequest;
import com.personalfinancetracker.dto.MessageResponse;
import com.personalfinancetracker.dto.RegisterRequest;
import com.personalfinancetracker.dto.ResendVerificationRequest;
import com.personalfinancetracker.dto.ResetPasswordRequest;
import com.personalfinancetracker.dto.VerifyEmailRequest;
import com.personalfinancetracker.dto.VerifyResetCodeRequest;
import com.personalfinancetracker.dto.VerifyResetCodeResponse;
import com.personalfinancetracker.security.AppUserPrincipal;
import com.personalfinancetracker.service.AuthService;
import com.personalfinancetracker.service.SessionAuthenticationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public AuthController(
            AuthService authService,
            SessionAuthenticationService sessionAuthenticationService) {
        this.authService = authService;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @GetMapping("/csrf")
    public CsrfInfo csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return new CsrfInfo(csrfToken.getHeaderName());
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new MessageResponse("If the address can be registered, a verification message will be sent."));
    }

    @PostMapping("/verify-email")
    public CurrentUserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return CurrentUserResponse.from(authService.verifyEmail(request.email(), request.code()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new MessageResponse("If verification is available, a message will be sent."));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authService.login(request);
        sessionAuthenticationService.establishSession(authentication, servletRequest, servletResponse);
        return new AuthResponse(CurrentUserResponse.from((AppUserPrincipal) authentication.getPrincipal()));
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        return CurrentUserResponse.from(principal);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new MessageResponse(authService.forgotPassword(request.email())));
    }

    @PostMapping("/verify-reset-code")
    public VerifyResetCodeResponse verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request) {
        return authService.verifyResetCode(request.email(), request.code());
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password changed. Sign in with your new password.");
    }
}