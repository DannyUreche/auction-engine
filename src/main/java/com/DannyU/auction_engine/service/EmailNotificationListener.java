package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

// Trimite pe email notificarile importante: oferta depasita, licitatie castigata, obiect vandut.
// Ruleaza dupa salvarea tranzactiei (nu trimitem email pentru o oferta anulata) si in fundal (@Async).
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    public record Email(String subject, String text) {
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(Notification notification) {
        buildEmail(notification).ifPresent(email ->
                emailService.send(notification.getUser().getEmail(), email.subject(), email.text()));
    }

    // Ce email (daca e cazul) trimitem pentru o notificare; NEW_BID si UNSOLD raman doar in clopotel
    public Optional<Email> buildEmail(Notification notification) {
        String subject = switch (notification.getType()) {
            case OUTBID -> "Oferta ta a fost depasita";
            case WON -> "Felicitari, ai castigat licitatia!";
            case SOLD -> "Obiectul tau s-a vandut";
            default -> null;
        };
        if (subject == null) {
            return Optional.empty();
        }
        String link = notification.getItemId() == null ? emailService.link("/") : emailService.link("/items/" + notification.getItemId());
        String text = "Salut, " + notification.getUser().getUsername() + "!\n\n"
                + notification.getMessage() + "\n\n"
                + "Vezi detaliile: " + link + "\n\n"
                + "-- \nLicitatii.live";
        return Optional.of(new Email(subject + " - Licitatii.live", text));
    }
}
