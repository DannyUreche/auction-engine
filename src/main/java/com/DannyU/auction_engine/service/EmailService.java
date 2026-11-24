package com.DannyU.auction_engine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Trimite emailuri prin SMTP. Daca serverul de email nu e configurat (MAIL_HOST lipseste),
// emailul doar se scrie in log - aplicatia merge la fel (util local si in teste).
@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String baseUrl;
    private final boolean configured;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${spring.mail.host:}") String host,
                        @Value("${app.mail.from}") String from,
                        @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.configured = !host.isBlank();
        this.from = from;
        this.baseUrl = baseUrl;
    }

    // Is an SMTP server configured? (if not, emails are only logged)
    public boolean isConfigured() {
        return configured;
    }

    public String link(String path) {
        return baseUrl + path;
    }

    public void send(String to, String subject, String text) {
        JavaMailSender sender = configured ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            log.info("[EMAIL - neconfigurat] Catre {}: {}\n{}", to, subject, text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            // Un email care nu pleaca nu trebuie sa strice licitatia
            log.warn("Emailul catre {} nu a putut fi trimis: {}", to, e.getMessage());
        }
    }
}
