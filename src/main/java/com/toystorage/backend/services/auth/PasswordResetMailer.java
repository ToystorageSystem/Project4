package com.toystorage.backend.services.auth;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
        message.setSubject("ToyStorage - Dat lai mat khau");
        // Fragment keeps the token out of the frontend HTTP request and Referer header.
        message.setText("Ban da yeu cau dat lai mat khau ToyStorage. Lien ket co han 15 phut:\n\n"
                +resetUrl+"#token="+token+"\n\nNeu da bat Google Authenticator, ban can ma OTP hien tai "
                +"hoac ma khoi phuc de doi mat khau va quet QR moi. Neu khong yeu cau, hay bo qua email nay.");
        mailSender.send(message);
    }
}
