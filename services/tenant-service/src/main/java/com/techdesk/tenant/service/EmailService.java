package com.techdesk.tenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Sends welcome and onboarding emails — captured by MailHog in local dev, real SMTP in production
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

    // Sends login credentials to the new Company Admin — email failure never blocks tenant creation
    public void sendWelcomeEmail(String adminEmail, String adminFirstName,
                                  String companyName, String temporaryPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("Welcome to TechDesk — Your Company Account is Ready");
            message.setText(buildWelcomeEmailBody(adminFirstName, companyName, adminEmail, temporaryPassword));

            mailSender.send(message);
            log.info("Welcome email dispatched to: {}", adminEmail);

        } catch (Exception ex) {
            log.error("Failed to send welcome email to {}. Tenant creation will proceed.", adminEmail, ex);
        }
    }

    private String buildWelcomeEmailBody(String firstName, String companyName,
                                          String email, String temporaryPassword) {
        return String.format(
            "Hello %s,%n%n" +
            "Your TechDesk account for %s has been successfully created.%n%n" +
            "You have been assigned the Company Admin role. Use the credentials below to log in " +
            "and begin configuring TechDesk for your organization.%n%n" +
            "Login URL: %s/login%n" +
            "Email: %s%n" +
            "Temporary Password: %s%n%n" +
            "You will be prompted to change your password upon first login.%n%n" +
            "If you did not expect this email, please contact support@techdesk.com immediately.%n%n" +
            "Regards,%n" +
            "The TechDesk Team",
            firstName, companyName, frontendUrl, email, temporaryPassword
        );
    }
}
