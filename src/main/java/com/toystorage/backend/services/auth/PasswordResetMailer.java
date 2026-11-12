package com.toystorage.backend.services.auth;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import java.net.URI;
import org.springframework.scheduling.annotation.Async;

@Service
public class PasswordResetMailer {
    private final ObjectProvider<JavaMailSender> sender;
    private final boolean enabled;
    private final String from;
    private final String resetUrl;
    public PasswordResetMailer(ObjectProvider<JavaMailSender> sender,
            @Value("${app.password-reset.mail-enabled:false}") boolean enabled,
            @Value("${app.password-reset.from:}") String from,
            @Value("${app.password-reset.frontend-url:http://localhost:5173/reset-password}") String resetUrl) {
        this.sender=sender; this.enabled=enabled; this.from=from; this.resetUrl=resetUrl;
        URI uri=URI.create(resetUrl);
        if (uri.getHost()==null || uri.getRawFragment()!=null || uri.getUserInfo()!=null
                || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())))
            throw new IllegalArgumentException("Configure a trusted frontend password-reset URL without fragment or userinfo");
    }
    public boolean isReady() { return enabled && !from.isBlank() && sender.getIfAvailable()!=null; }
    /** Synchronous delivery lets the caller roll back a password change on SMTP failure. */
    public void sendCredentials(String email, String password, boolean newAccount) {
        JavaMailSender mailSender = sender.getIfAvailable();
        if (!isReady() || mailSender == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "RESET_EMAIL_NOT_CONFIGURED");
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(newAccount ? "ToyStorage - Your sign-in details" : "ToyStorage - Your temporary password");
            String plain = "Hello,\n\n"
                    + (newAccount ? "Your ToyStorage account has been created." : "Your temporary password is ready.")
                    + "\nSign-in email: " + email + "\nTemporary password: " + password
                    + "\n\nSign in with these details and change your password when prompted."
                    + "\nIf Google Authenticator is enabled, enter your verification code to complete sign-in.";
            helper.setText(plain, CredentialEmailTemplate.render(email, password, newAccount));
            mailSender.send(message);
        } catch (MessagingException | RuntimeException failure) {
            // SMTP exception details can contain the message and credentials.
            org.slf4j.LoggerFactory.getLogger(PasswordResetMailer.class)
                    .error("Credential email delivery failed; check SMTP configuration and connectivity");
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_DELIVERY_FAILED");
        }
    }
    @Async("passwordResetMailExecutor")
    public void send(String email, String token) {
        try { sendMessage(email,token); }
        catch (RuntimeException ignored) {
            // Async exception handlers may otherwise print method arguments including the reset token.
            org.slf4j.LoggerFactory.getLogger(PasswordResetMailer.class)
                    .error("Password reset email delivery failed; check SMTP configuration and connectivity");
        }
    }
    private void sendMessage(String email, String token) {
        JavaMailSender mailSender=sender.getIfAvailable();
        if (!isReady() || mailSender==null) throw new IllegalStateException("Password reset mail is not configured");
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject("ToyStorage - Reset your password");
        // Fragment keeps the token out of the frontend HTTP request and Referer header.
        message.setText("You requested a ToyStorage password reset. This link expires in 15 minutes:\n\n"
                +resetUrl+"#token="+token+"\n\nIf Google Authenticator is enabled, you will need your current verification code "
                +"or a recovery code to change your password and scan a new QR code. If you did not request this, ignore this email.");
        mailSender.send(message);
    }
}
