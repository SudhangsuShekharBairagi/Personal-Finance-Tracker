package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.personalfinancetracker.exception.EmailDeliveryException;

class MailServiceTest {
    @Test
    void failsClearlyWhenSmtpIsNotConfigured() {
        JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
        MailService mailService = new MailService(sender, "", "");

        assertThrows(
                EmailDeliveryException.class,
                () -> mailService.sendVerificationCode("demo@example.test", "Demo", "123456", 10));
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void wrapsSmtpFailuresWithoutExposingTheCode() {
        JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .when(sender).send(any(SimpleMailMessage.class));
        MailService mailService = new MailService(sender, "smtp.example.test", "noreply@example.test");

        assertThrows(
                EmailDeliveryException.class,
                () -> mailService.sendPasswordResetCode("demo@example.test", "Demo", "654321", 10));
        verify(sender).send(any(SimpleMailMessage.class));
    }
}