package com.personalfinancetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.personalfinancetracker.exception.EmailDeliveryException;

@Service
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String email, String name, String code, int expiryMinutes) {
        send(
                email,
                "Verify your Personal Finance Tracker email",
                "Hello " + name + ",\n\nYour email verification code is " + code
                        + ". It expires in " + expiryMinutes + " minutes and can be used once.\n\n"
                        + "If you did not request this code, you can ignore this email.");
    }

    public void sendPasswordResetCode(String email, String name, String code, int expiryMinutes) {
        send(
                email,
                "Your password reset code",
                "Hello " + name + ",\n\nYour password reset code is " + code
                        + ". It expires in " + expiryMinutes + " minutes and can be used once.\n\n"
                        + "If you did not request this code, you can ignore this email.");
    }

    public void sendBudgetAlert(String email, String category, String period, String threshold, String spending,
            String limit) {
        String safeCategory = escape(category);
        String safePeriod = escape(period);
        String safeThreshold = escape(threshold);
        String safeSpending = escape(spending);
        String safeLimit = escape(limit);
        String body = "Budget alert for " + safeCategory + " in " + safePeriod + ".\n\n"
                + "Spending: ₹" + safeSpending + "\n"
                + "Limit: ₹" + safeLimit + "\n"
                + "Threshold reached: " + safeThreshold + "%\n\n"
                + "This message is informational only and does not automatically change your budget or payment status.";
        send(email, "Budget alert: " + safeCategory + " reached " + safeThreshold + "%", body);
    }

    private void send(String email, String subject, String body) {
        if (mailHost.isBlank() || fromAddress.isBlank()) {
            logger.warn("Email was not sent because SMTP host or sender is not configured.");
            throw new EmailDeliveryException("Email delivery is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.warn("Email delivery failed using the configured SMTP server.");
            throw new EmailDeliveryException("Email delivery failed", exception);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", " ").replace("\r", " ");
    }
}