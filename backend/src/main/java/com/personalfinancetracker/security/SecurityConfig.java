package com.personalfinancetracker.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import com.personalfinancetracker.dto.ApiError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {
        @Bean
        CookieCsrfTokenRepository csrfTokenRepository(
                        @Value("${app.cookie-secure:false}") boolean secureCookie) {
                CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                repository.setCookieName("XSRF-TOKEN");
                repository.setHeaderName("X-XSRF-TOKEN");
                repository.setCookieCustomizer(cookie -> cookie
                                .path("/")
                                .sameSite("Lax")
                                .secure(secureCookie)
                                .httpOnly(false));
                return repository;
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource(
                        @Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendOrigin));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Accept", "Content-Type", "X-XSRF-TOKEN"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        CookieCsrfTokenRepository csrfTokenRepository,
                        CorsConfigurationSource corsConfigurationSource,
                        SessionRegistry sessionRegistry,
                        ObjectMapper objectMapper) throws Exception {
                ExpiredSessionGuardFilter expiredSessionGuardFilter = new ExpiredSessionGuardFilter(sessionRegistry,
                                objectMapper);
                return http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(csrfTokenRepository)
                                                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                                .maximumSessions(10)
                                                .sessionRegistry(sessionRegistry))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, exception) -> writeError(
                                                                objectMapper,
                                                                request,
                                                                response,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "AUTHENTICATION_REQUIRED",
                                                                "Sign in to access this resource."))
                                                .accessDeniedHandler(jsonAccessDeniedHandler(objectMapper)))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(request -> CorsUtils.isPreFlightRequest(request))
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/health", "/api/auth/csrf")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/auth/register",
                                                                "/api/auth/verify-email",
                                                                "/api/auth/resend-verification",
                                                                "/api/auth/login",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/verify-reset-code",
                                                                "/api/auth/reset-password")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .logout(logout -> logout
                                                .logoutUrl("/api/auth/logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                                                .logoutSuccessHandler((request, response, authentication) -> response
                                                                .setStatus(HttpServletResponse.SC_NO_CONTENT)))
                                .addFilterAfter(expiredSessionGuardFilter,
                                                org.springframework.security.web.context.SecurityContextHolderFilter.class)
                                .build();
        }

        private AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
                return (request, response, exception) -> writeError(
                                objectMapper,
                                request,
                                response,
                                HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "This request is not allowed.");
        }

        private void writeError(
                        ObjectMapper objectMapper,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        HttpStatus status,
                        String code,
                        String message) throws IOException {
                response.setStatus(status.value());
                response.setContentType("application/json");
                objectMapper.writeValue(
                                response.getOutputStream(),
                                new ApiError(Instant.now(), status.value(), code, message, Map.of()));
        }

        private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
                private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
                private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

                @Override
                public void handle(
                                HttpServletRequest request,
                                HttpServletResponse response,
                                java.util.function.Supplier<org.springframework.security.web.csrf.CsrfToken> deferredCsrfToken) {
                        xor.handle(request, response, deferredCsrfToken);
                        deferredCsrfToken.get();
                }

                @Override
                public String resolveCsrfTokenValue(
                                HttpServletRequest request,
                                org.springframework.security.web.csrf.CsrfToken csrfToken) {
                        String headerValue = request.getHeader(csrfToken.getHeaderName());
                        return StringUtils.hasText(headerValue)
                                        ? plain.resolveCsrfTokenValue(request, csrfToken)
                                        : xor.resolveCsrfTokenValue(request, csrfToken);
                }
        }
}