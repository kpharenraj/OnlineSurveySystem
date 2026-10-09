package com.surveysystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String host;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.host = host.trim();
        this.from = from.trim();
    }

    public boolean isConfigured() {
        return !host.isBlank() && !from.isBlank();
    }

    public boolean sendWelcome(String name, String email) {
        if (!isConfigured()) {
            logger.warn("Welcome email was not sent because SMTP_HOST or MAIL_FROM is not configured.");
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Welcome to Gather");
        message.setText("Hi " + name + ",\n\n"
                + "Welcome to Gather! Your account is ready. You can now create surveys, "
                + "share them with respondents, and explore the results.\n\n"
                + "Thanks,\nThe Gather team");
        try {
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            logger.error("Could not deliver a Gather welcome email.", exception);
            return false;
        }
    }

    public void sendPasswordResetCode(String email, String code) {
        if (!isConfigured()) {
            throw new EmailNotConfiguredException();
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Your Gather password reset code");
        message.setText("We received a request to reset the password for this email address.\n\n"
                + "Your one-time code is: " + code + "\n\n"
                + "The code expires in 10 minutes and can only be used once. "
                + "If you did not request a password reset, you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.error("Could not deliver a Gather password reset email.", exception);
            throw new EmailDeliveryException(exception);
        }
    }

    public static class EmailNotConfiguredException extends RuntimeException {
    }

    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(Throwable cause) {
            super(cause);
        }
    }
}
