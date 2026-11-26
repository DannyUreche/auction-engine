package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The user's own data: a full export (GDPR "right of access") and account deletion.
// Deletion anonymizes instead of removing the row: bids, sales, reviews and chat messages also belong to
// the other people involved, so they stay, but under "sters_<id>" with no email and no way to log in.
// Everything that only concerns the user (notifications, watchlist, auto-bids, lists, push, tokens) is deleted.
@Service
@RequiredArgsConstructor
public class AccountService {

    public static final String DELETED_PREFIX = "sters_";

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final com.DannyU.auction_engine.repository.PurchaseOrderRepository orderRepository;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<String, Object> export(String username) {
        User user = find(username);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportat_la", LocalDateTime.now());
        data.put("cont", Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "creat_la", user.getCreatedAt(),
                "email_confirmat", user.isEmailVerified(),
                "sold_portofel", user.getBalance()));
        data.put("obiecte_puse_la_vanzare", rows("""
                SELECT i.id, i.name, i.description, i.category, i.auctionType, i.startPrice, i.currentPrice, i.buyNowPrice,
                       i.active, i.startTime, i.endTime, w.username
                FROM Item i LEFT JOIN i.winner w WHERE i.seller = :user ORDER BY i.id""", user,
                "id", "nume", "descriere", "categorie", "tip", "pret_pornire", "pret_curent", "cumpara_acum",
                "activ", "inceput", "sfarsit", "castigator"));
        data.put("oferte", rows("""
                SELECT b.item.id, b.item.name, b.amount, b.automatic, b.createdAt
                FROM Bid b WHERE b.bidder = :user ORDER BY b.createdAt""", user,
                "obiect_id", "obiect", "suma", "automata", "data"));
        data.put("licitare_automata", rows("""
                SELECT a.item.id, a.item.name, a.maxAmount, a.createdAt FROM AutoBid a WHERE a.user = :user""", user,
                "obiect_id", "obiect", "suma_maxima", "data"));
        data.put("obiecte_urmarite", rows("""
                SELECT w.item.id, w.item.name, w.createdAt FROM WatchedItem w WHERE w.user = :user""", user,
                "obiect_id", "obiect", "data"));
        data.put("comenzi", rows("""
                SELECT o.item.id, o.item.name, o.amount, o.status, o.courier, o.awb, o.createdAt FROM PurchaseOrder o
                WHERE o.buyer = :user OR o.seller = :user ORDER BY o.createdAt""", user,
                "obiect_id", "obiect", "suma", "stare", "curier", "awb", "data"));
        data.put("colectii", rows("""
                SELECT c.id, c.title, c.description, c.publicList, c.createdAt FROM ItemCollection c WHERE c.owner = :user""", user,
                "id", "titlu", "descriere", "publica", "data"));
        data.put("intrebari", rows("""
                SELECT q.item.id, q.text, q.answer, q.createdAt FROM Question q WHERE q.author = :user ORDER BY q.createdAt""", user,
                "obiect_id", "intrebare", "raspuns", "data"));
        data.put("mesaje_trimise", rows("""
                SELECT m.item.id, m.text, m.createdAt FROM Message m WHERE m.sender = :user ORDER BY m.createdAt""", user,
                "obiect_id", "text", "data"));
        data.put("recenzii_scrise", rows("""
                SELECT r.item.id, r.target.username, r.rating, r.comment, r.createdAt FROM Review r WHERE r.author = :user""", user,
                "obiect_id", "pentru", "nota", "comentariu", "data"));
        data.put("recenzii_primite", rows("""
                SELECT r.item.id, r.author.username, r.rating, r.comment, r.createdAt FROM Review r WHERE r.target = :user""", user,
                "obiect_id", "de_la", "nota", "comentariu", "data"));
        data.put("tranzactii_portofel", rows("""
                SELECT t.type, t.amount, t.itemId, t.description, t.createdAt
                FROM WalletTransaction t WHERE t.user = :user ORDER BY t.createdAt""", user,
                "tip", "suma", "obiect_id", "descriere", "data"));
        data.put("notificari", rows("""
                SELECT n.type, n.message, n.itemId, n.read, n.createdAt
                FROM Notification n WHERE n.user = :user ORDER BY n.createdAt""", user,
                "tip", "mesaj", "obiect_id", "citita", "data"));
        data.put("raportari", rows("""
                SELECT r.item.id, r.reason, r.details, r.createdAt FROM Report r WHERE r.reporter = :user""", user,
                "obiect_id", "motiv", "detalii", "data"));
        return data;
    }

    @Transactional
    public void delete(String username, String confirmation) {
        if (!username.equals(confirmation == null ? null : confirmation.trim())) {
            throw new IllegalArgumentException("Scrie exact numele tau de utilizator ca sa confirmi stergerea contului!");
        }
        User user = find(username);
        if (orderRepository.openFor(username) > 0) {
            throw new IllegalArgumentException("Ai comenzi in desfasurare (cu bani in escrow). Poti sterge contul dupa ce se incheie.");
        }
        if (itemRepository.reservedBy(username).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Conduci cel putin o licitatie activa. Poti sterge contul dupa ce se incheie.");
        }
        List<Item> active = entityManager.createQuery(
                        "SELECT i FROM Item i WHERE i.seller = :user AND i.active = true", Item.class)
                .setParameter("user", user).getResultList();
        boolean withBids = active.stream().anyMatch(item -> !entityManager.createQuery(
                        "SELECT b.id FROM Bid b WHERE b.item = :item")
                .setParameter("item", item).setMaxResults(1).getResultList().isEmpty());
        if (withBids) {
            throw new IllegalArgumentException("Ai obiecte la vanzare care au deja oferte. Poti sterge contul dupa ce se incheie licitatiile.");
        }
        active.forEach(item -> adminService.deleteItem(item.getId()));

        for (String entity : List.of("WatchedItem", "AutoBid", "Notification", "PushSubscription",
                "PasswordResetToken", "EmailVerificationToken")) {
            entityManager.createQuery("DELETE FROM " + entity + " e WHERE e.user = :user")
                    .setParameter("user", user).executeUpdate();
        }

        // Personal lists go too (their items are deleted by the cascade in the database)
        entityManager.createQuery("DELETE FROM CollectionItem ci WHERE ci.collection.owner = :user")
                .setParameter("user", user).executeUpdate();
        entityManager.createQuery("DELETE FROM ItemCollection c WHERE c.owner = :user")
                .setParameter("user", user).executeUpdate();

        user.setUsername(DELETED_PREFIX + user.getId());
        user.setEmail(DELETED_PREFIX + user.getId() + "@sters.invalid");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEmailVerified(false);
        user.setBlocked(true);
        userRepository.save(user);
    }

    private User find(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
    }

    // Runs a JPQL query for this user and turns each row into a map with the given (Romanian) keys
    private List<Map<String, Object>> rows(String jpql, User user, String... keys) {
        List<?> result = entityManager.createQuery(jpql).setParameter("user", user).getResultList();
        return result.stream().map(row -> {
            Object[] values = row instanceof Object[] array ? array : new Object[]{row};
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], values[i]);
            }
            return map;
        }).toList();
    }
}
