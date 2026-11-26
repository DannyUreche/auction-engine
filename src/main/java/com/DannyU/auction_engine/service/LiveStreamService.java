package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.LiveShow;
import com.DannyU.auction_engine.repository.LiveShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Video for live shows: the seller's camera goes straight from browser to browser (WebRTC), this class only
// passes the connection messages (offer / answer / ICE candidates). Each message waits in the private inbox of
// its recipient; the public event stream only says "peer X has mail" (SSE "rtc", no content), so SDP and IP
// addresses never reach other visitors. Viewer peers are random ids made by the browser (a capability).
// Everything is in memory, per instance. Peer-to-peer means the seller uploads one copy per viewer: max 8.
@Service
@RequiredArgsConstructor
public class LiveStreamService {

    static final int MAX_VIEWERS = 8;
    static final int MAX_INBOX = 200;
    static final int MAX_DATA = 20_000;
    static final long MESSAGE_TTL_MS = 120_000;
    static final long HOST_TIMEOUT_MS = 45_000;
    private static final Set<String> KINDS = Set.of("offer", "answer", "ice");

    private final LiveShowRepository showRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<Integer, Stream> streams = new ConcurrentHashMap<>();
    private final Map<String, Deque<Signal>> inboxes = new ConcurrentHashMap<>();

    public record Signal(String from, String kind, String data, long at) {
    }

    public record Status(boolean live, int viewers, int maxViewers) {
    }

    private static final class Stream {
        final Set<String> viewers = ConcurrentHashMap.newKeySet();
        volatile long hostSeen = System.currentTimeMillis();
    }

    public static String hostPeer(Integer showId) {
        return "host-" + showId;
    }

    public Status status(Integer showId) {
        Stream stream = streams.get(showId);
        return new Status(stream != null, stream == null ? 0 : stream.viewers.size(), MAX_VIEWERS);
    }

    public Status start(Integer showId, String username) {
        requireSeller(showId, username);
        streams.computeIfAbsent(showId, id -> new Stream()).hostSeen = System.currentTimeMillis();
        eventPublisher.publishEvent(AuctionEvent.stream(showId, true));
        return status(showId);
    }

    public void stop(Integer showId, String username) {
        requireSeller(showId, username);
        end(showId);
    }

    // A viewer wants the video: the host's browser gets a "join" and answers with an offer
    public Status join(Integer showId, String peer) {
        Stream stream = live(showId);
        checkPeer(peer);
        if (!stream.viewers.contains(peer) && stream.viewers.size() >= MAX_VIEWERS) {
            throw new IllegalArgumentException("Transmisiunea video are deja numarul maxim de " + MAX_VIEWERS + " privitori. Poti licita in continuare.");
        }
        stream.viewers.add(peer);
        deliver(showId, hostPeer(showId), new Signal(peer, "join", null, System.currentTimeMillis()));
        return status(showId);
    }

    public void leave(Integer showId, String peer) {
        Stream stream = streams.get(showId);
        if (stream != null && peer != null && stream.viewers.remove(peer)) {
            inboxes.remove(peer);
            deliver(showId, hostPeer(showId), new Signal(peer, "leave", null, System.currentTimeMillis()));
        }
    }

    // offer / ICE from the host to one viewer, answer / ICE from a viewer to the host
    public void signal(Integer showId, String from, String to, String kind, String data, String username) {
        Stream stream = live(showId);
        if (!KINDS.contains(kind)) {
            throw new IllegalArgumentException("Mesaj video necunoscut.");
        }
        if (data == null || data.length() > MAX_DATA) {
            throw new IllegalArgumentException("Mesaj video invalid.");
        }
        String host = hostPeer(showId);
        if (host.equals(from)) {
            requireSeller(showId, username);
            if (!stream.viewers.contains(to)) {
                throw new IllegalArgumentException("Privitorul nu mai este conectat.");
            }
        } else if (!stream.viewers.contains(from) || !host.equals(to)) {
            throw new IllegalArgumentException("Nu esti conectat la aceasta transmisiune.");
        }
        deliver(showId, to, new Signal(from, kind, data, System.currentTimeMillis()));
    }

    // Takes the waiting messages of a peer (the host's inbox only for the seller)
    public List<Signal> inbox(Integer showId, String peer, String username) {
        if (hostPeer(showId).equals(peer)) {
            requireSeller(showId, username);
            Stream stream = streams.get(showId);
            if (stream != null) {
                stream.hostSeen = System.currentTimeMillis();
            }
        } else {
            checkPeer(peer);
        }
        Deque<Signal> inbox = inboxes.remove(peer);
        if (inbox == null) {
            return List.of();
        }
        synchronized (inbox) {
            long oldest = System.currentTimeMillis() - MESSAGE_TTL_MS;
            return inbox.stream().filter(signal -> signal.at() >= oldest).toList();
        }
    }

    // The seller closed the tab without stopping: end the stream once their browser stops polling
    @Scheduled(fixedRate = 15_000)
    public void cleanUp() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Stream> entry : new ArrayList<>(streams.entrySet())) {
            if (now - entry.getValue().hostSeen > HOST_TIMEOUT_MS) {
                end(entry.getKey());
            }
        }
        inboxes.values().removeIf(inbox -> {
            synchronized (inbox) {
                return inbox.isEmpty() || now - inbox.peekLast().at() > MESSAGE_TTL_MS;
            }
        });
    }

    private void end(Integer showId) {
        Stream stream = streams.remove(showId);
        if (stream != null) {
            stream.viewers.forEach(inboxes::remove);
            inboxes.remove(hostPeer(showId));
            eventPublisher.publishEvent(AuctionEvent.stream(showId, false));
        }
    }

    private void deliver(Integer showId, String to, Signal signal) {
        Deque<Signal> inbox = inboxes.computeIfAbsent(to, key -> new ArrayDeque<>());
        synchronized (inbox) {
            if (inbox.size() >= MAX_INBOX) {
                inbox.pollFirst();
            }
            inbox.addLast(signal);
        }
        eventPublisher.publishEvent(AuctionEvent.rtc(showId, to));
    }

    private Stream live(Integer showId) {
        Stream stream = streams.get(showId);
        if (stream == null) {
            throw new IllegalArgumentException("Vanzatorul nu transmite video acum.");
        }
        return stream;
    }

    private static void checkPeer(String peer) {
        if (peer == null || !peer.matches("[a-f0-9-]{32,40}")) {
            throw new IllegalArgumentException("Identificator de privitor invalid.");
        }
    }

    private void requireSeller(Integer showId, String username) {
        LiveShow show = showRepository.findById(showId).orElseThrow(() -> new IllegalArgumentException("Licitatia live nu a fost gasita!"));
        if (username == null || !username.equals(show.getSellerUsername())) {
            throw new IllegalArgumentException("Doar organizatorul poate transmite video.");
        }
    }
}
