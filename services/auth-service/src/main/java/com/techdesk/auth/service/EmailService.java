package com.techdesk.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Sends transactional emails — MailHog captures all mail in local dev at http://localhost:8025
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    // Sends a 15-minute reset link — silently ignores send failures to prevent email enumeration
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@techdesk.com");
        message.setTo(toEmail);
        message.setSubject("TechDesk — Password Reset Request");
        message.setText(
            "Hello,\n\n"
            + "We received a request to reset your TechDesk password.\n\n"
            + "Click the link below to set a new password (expires in 15 minutes):\n"
            + resetLink + "\n\n"
            + "If you did not request this, please ignore this email.\n\n"
            + "— TechDesk Security Team"
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }
}
