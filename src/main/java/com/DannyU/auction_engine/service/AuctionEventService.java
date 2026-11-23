package com.DannyU.auction_engine.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Tine evidenta browserelor conectate la /api/events si le trimite evenimentele licitatiilor (Server-Sent Events)
@Service
public class AuctionEventService {

    // Dupa 30 de minute conexiunea se inchide; browserul (EventSource) se reconecteaza singur
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        // Primul mesaj confirma browserului ca e conectat
        send(emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    // Se apeleaza abia DUPA ce tranzactia a fost salvata in baza, ca browserul sa citeasca date actualizate.
    // fallbackExecution = evenimentul e trimis si cand e publicat in afara unei tranzactii.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuctionEvent(AuctionEvent event) {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().name(event.type()).data(event));
        }
    }

    // Un mesaj gol la 25 de secunde tine conexiunea deschisa si elimina browserele deconectate
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().comment("ping"));
        }
    }

    public int connectedClients() {
        return emitters.size();
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // Browserul s-a deconectat
            emitters.remove(emitter);
        }
    }
}
