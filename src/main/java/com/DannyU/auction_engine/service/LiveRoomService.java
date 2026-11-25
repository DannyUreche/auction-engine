package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.LiveComment;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.LiveCommentRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Live auction room: who is watching an item right now (each open page sends a heartbeat every 15 s with a
// random id of the tab) and the public live chat. Presence is kept in memory (one instance on Render).
@Service
@RequiredArgsConstructor
public class LiveRoomService {

    static final long PRESENCE_TTL_MS = 45_000;
    static final long COMMENT_INTERVAL_MS = 3_000;

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final LiveCommentRepository liveCommentRepository;
    private final ApplicationEventPublisher events;
    private final EntityManager entityManager;

    // itemId -> (tab id -> last heartbeat)
    private final Map<Integer, Map<String, Long>> viewers = new ConcurrentHashMap<>();
    private final Map<String, Long> lastComment = new ConcurrentHashMap<>();

    // Registers a heartbeat and returns how many tabs are watching. The first heartbeat of a tab counts as a
    // view (except the seller's own visits).
    @Transactional
    public int heartbeat(Integer itemId, String session, String username) {
        if (session == null || session.isBlank() || session.length() > 64) {
            throw new IllegalArgumentException("Sesiune invalida!");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        long now = System.currentTimeMillis();
        Map<String, Long> tabs = viewers.computeIfAbsent(itemId, key -> new ConcurrentHashMap<>());
        tabs.values().removeIf(seen -> now - seen > PRESENCE_TTL_MS);
        boolean newVisit = tabs.put(session, now) == null;
        if (newVisit && !item.getSeller().getUsername().equals(username)) {
            entityManager.createQuery("UPDATE Item i SET i.viewCount = i.viewCount + 1 WHERE i.id = :id")
                    .setParameter("id", itemId).executeUpdate();
        }
        return tabs.size();
    }

    public void leave(Integer itemId, String session) {
        Map<String, Long> tabs = viewers.get(itemId);
        if (tabs != null && session != null) {
            tabs.remove(session);
        }
    }

    public List<LiveComment> comments(Integer itemId) {
        List<LiveComment> newest = new ArrayList<>(liveCommentRepository.findTop50ByItemIdOrderByCreatedAtDesc(itemId));
        java.util.Collections.reverse(newest);
        return newest;
    }

    @Transactional
    public LiveComment comment(Integer itemId, String username, String text) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new IllegalArgumentException("Chatul live este deschis doar cat timp dureaza licitatia.");
        }
        long now = System.currentTimeMillis();
        Long previous = lastComment.get(username);
        if (previous != null && now - previous < COMMENT_INTERVAL_MS) {
            throw new IllegalArgumentException("Mai asteapta putin intre mesaje.");
        }
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        LiveComment comment = new LiveComment();
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setText(text.trim());
        liveCommentRepository.save(comment);
        lastComment.put(username, now);
        events.publishEvent(AuctionEvent.comment(itemId, username));
        return comment;
    }
}
