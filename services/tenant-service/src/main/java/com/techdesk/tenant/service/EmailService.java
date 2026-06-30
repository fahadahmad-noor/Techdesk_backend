package com.techdesk.tenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Handles outbound email communication for the tenant-service.
 *
 * In local development, all emails are captured by MailHog (http://localhost:8025)
 * and never delivered to real recipients. In production, this service connects to
 * the configured SMTP server via JavaMailSender.
 */
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

    /**
     * Sends a welcome and account-setup email to the newly created Company Admin.
     *
     * The email instructs the admin to set their password and begin configuring their
     * TechDesk environment. The temporary password is included as a first-login credential.
     *
     * @param adminEmail      the Company Admin's email address
     * @param adminFirstName  the Company Admin's first name (used in the greeting)
     * @param companyName     the name of the newly onboarded company
     * @param temporaryPassword the system-generated temporary password for first login
     */
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
            // Email delivery failure must never prevent the tenant from being created.
            // The admin account already exists — the password can be reset via the forgot-password flow.
            log.error("Failed to send welcome email to {}. Tenant creation will proceed.", adminEmail, ex);
        }
    }

    /**
     * Constructs the plain-text body of the welcome email.
     */
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
