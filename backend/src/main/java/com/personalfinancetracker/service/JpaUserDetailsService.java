package com.personalfinancetracker.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.security.AppUserPrincipal;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final AppUserRepository userRepository;

    public JpaUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(java.util.Locale.ROOT);
    }
}