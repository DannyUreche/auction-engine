package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuditEntry;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.PurchaseOrder;
import com.DannyU.auction_engine.entity.PurchaseOrder.Status;
import com.DannyU.auction_engine.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Orders with escrow. Closing an auction with a winner creates a PAID order: the price leaves the buyer's
// wallet but reaches the seller only when the buyer confirms delivery. Deadlines protect both sides:
// unshipped after SHIP_DAYS -> refunded to the buyer; shipped but not confirmed after CONFIRM_DAYS -> released
// to the seller. A buyer can open a dispute while the order is open; an admin decides (refund or release).
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final int SHIP_DAYS = 5;
    public static final int CONFIRM_DAYS = 7;
    public static final List<String> COURIERS = List.of("FAN Courier", "Sameday", "Cargus", "DPD", "GLS", "Posta Romana", "Predare personala");
    static final String PICKUP = "Predare personala";

    private final PurchaseOrderRepository orderRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AdminAccess adminAccess;

    // What the browser sees (no emails)
    public record OrderView(Integer id, Integer itemId, String itemName, String itemImageUrl, String buyer, String seller,
                            BigDecimal amount, Status status, String courier, String awb, String disputeReason, String resolution,
                            LocalDateTime createdAt, LocalDateTime shippedAt, LocalDateTime disputedAt, LocalDateTime closedAt,
                            LocalDateTime shipBy, LocalDateTime autoConfirmAt, String role) {
    }

    public record MyOrders(List<OrderView> purchases, List<OrderView> sales) {
    }

    // Called by AuctionScheduler.closeAuction when there is a winner
    @Transactional
    public PurchaseOrder open(Item item) {
        walletService.hold(item);
        PurchaseOrder order = new PurchaseOrder();
        order.setItem(item);
        order.setBuyer(item.getWinner());
        order.setSeller(item.getSeller());
        order.setAmount(item.getCurrentPrice());
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Optional<OrderView> forItem(Integer itemId, String username) {
        return orderRepository.findByItem_Id(itemId)
                .filter(order -> participant(order, username) || adminAccess.isAdmin(username))
                .map(order -> view(order, username));
    }

    @Transactional(readOnly = true)
    public MyOrders mine(String username) {
        return new MyOrders(
                orderRepository.findByBuyer_UsernameOrderByCreatedAtDesc(username).stream().map(o -> view(o, username)).toList(),
                orderRepository.findBySeller_UsernameOrderByCreatedAtDesc(username).stream().map(o -> view(o, username)).toList());
    }

    @Transactional(readOnly = true)
    public List<OrderView> disputes(String admin) {
        return orderRepository.findByStatusOrderByDisputedAtAsc(Status.DISPUTED).stream().map(o -> view(o, admin)).toList();
    }

    // Seller: the item left (courier + AWB, or handed over in person)
    @Transactional
    public OrderView ship(Integer orderId, String username, String courier, String awb) {
        PurchaseOrder order = find(orderId);
        requireSeller(order, username);
        if (order.getStatus() != Status.PAID) {
            throw new IllegalArgumentException("Comanda nu mai asteapta expedierea.");
        }
        String cleanCourier = courier == null ? "" : courier.trim();
        if (!COURIERS.contains(cleanCourier)) {
            throw new IllegalArgumentException("Alege curierul din lista.");
        }
        String cleanAwb = awb == null ? "" : awb.trim();
        if (!PICKUP.equals(cleanCourier) && !cleanAwb.matches("[A-Za-z0-9-]{4,40}")) {
            throw new IllegalArgumentException("Scrie numarul AWB (4-40 de litere, cifre sau cratime).");
        }
        order.setCourier(cleanCourier);
        order.setAwb(PICKUP.equals(cleanCourier) ? null : cleanAwb);
        order.setStatus(Status.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);
        String how = PICKUP.equals(cleanCourier) ? "a marcat predarea personala" : "l-a trimis prin " + cleanCourier + " (AWB " + cleanAwb + ")";
        notificationService.notify(order.getBuyer(), Notification.Type.ORDER_SHIPPED, order.getItem(),
                order.getSeller().getUsername() + " " + how + ": \"" + order.getItem().getName() + "\". Confirma primirea cand il ai.");
        return view(order, username);
    }

    // Buyer: received and fine -> the seller gets the money
    @Transactional
    public OrderView confirm(Integer orderId, String username) {
        PurchaseOrder order = find(orderId);
        requireBuyer(order, username);
        if (order.getStatus() != Status.SHIPPED && order.getStatus() != Status.PAID) {
            throw new IllegalArgumentException("Comanda nu poate fi confirmata acum.");
        }
        complete(order, "Cumparatorul a confirmat primirea.");
        return view(order, username);
    }

    // Buyer: something is wrong -> an admin decides; the money stays in escrow meanwhile
    @Transactional
    public OrderView dispute(Integer orderId, String username, String reason) {
        PurchaseOrder order = find(orderId);
        requireBuyer(order, username);
        if (order.getStatus() != Status.PAID && order.getStatus() != Status.SHIPPED) {
            throw new IllegalArgumentException("Pentru aceasta comanda nu se mai poate deschide o disputa.");
        }
        String text = reason == null ? "" : reason.trim();
        if (text.length() < 10 || text.length() > 1000) {
            throw new IllegalArgumentException("Descrie problema in 10-1000 de caractere.");
        }
        order.setStatus(Status.DISPUTED);
        order.setDisputeReason(text);
        order.setDisputedAt(LocalDateTime.now());
        orderRepository.save(order);
        notificationService.notify(order.getSeller(), Notification.Type.ORDER_DISPUTE, order.getItem(),
                order.getBuyer().getUsername() + " a deschis o disputa pentru \"" + order.getItem().getName()
                        + "\". Banii raman blocati pana decide un administrator.");
        return view(order, username);
    }

    // Admin: refund the buyer or release the money to the seller
    @Transactional
    public OrderView resolve(Integer orderId, String admin, boolean refund, String note) {
        PurchaseOrder order = find(orderId);
        if (order.getStatus() != Status.DISPUTED) {
            throw new IllegalArgumentException("Comanda nu este in disputa.");
        }
        String text = note == null || note.isBlank() ? (refund ? "Banii au fost returnati cumparatorului." : "Banii au fost eliberati vanzatorului.") : note.trim();
        if (refund) {
            refund(order, "Decizia administratorului: " + text);
        } else {
            complete(order, "Decizia administratorului: " + text);
        }
        String message = "Disputa pentru \"" + order.getItem().getName() + "\" a fost rezolvata: "
                + (refund ? "banii se intorc la cumparator. " : "banii merg la vanzator. ") + text;
        notificationService.notify(order.getBuyer(), Notification.Type.ORDER_RESOLVED, order.getItem(), message);
        notificationService.notify(order.getSeller(), Notification.Type.ORDER_RESOLVED, order.getItem(), message);
        auditService.record(admin, AuditEntry.Action.RESOLVE_DISPUTE, "ORDER", order.getId(), order.getItem().getName(),
                (refund ? "rambursat cumparatorului " : "eliberat vanzatorului ") + order.getBuyer().getUsername() + " / " + order.getSeller().getUsername());
        return view(order, admin);
    }

    // An admin deletes a sold item: an open order is refunded first, so no money is lost
    @Transactional
    public void refundBeforeDeletion(Integer itemId) {
        orderRepository.findByItem_Id(itemId).filter(PurchaseOrder::isOpen)
                .ifPresent(order -> refund(order, "anuntul a fost sters de un administrator"));
    }

    // Deadlines, every 10 minutes
    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    @Transactional
    public void applyDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        for (PurchaseOrder order : orderRepository.findByStatusAndCreatedAtBefore(Status.PAID, now.minusDays(SHIP_DAYS))) {
            refund(order, "vanzatorul nu a expediat in " + SHIP_DAYS + " zile");
            notificationService.notify(order.getSeller(), Notification.Type.ORDER_RESOLVED, order.getItem(),
                    "Comanda pentru \"" + order.getItem().getName() + "\" a fost anulata: nu ai expediat in " + SHIP_DAYS + " zile.");
        }
        for (PurchaseOrder order : orderRepository.findByStatusAndShippedAtBefore(Status.SHIPPED, now.minusDays(CONFIRM_DAYS))) {
            complete(order, "Confirmata automat la " + CONFIRM_DAYS + " zile de la expediere.");
        }
    }

    private void complete(PurchaseOrder order, String resolution) {
        walletService.release(order);
        order.setStatus(Status.COMPLETED);
        order.setResolution(resolution);
        order.setClosedAt(LocalDateTime.now());
        orderRepository.save(order);
        notificationService.notify(order.getSeller(), Notification.Type.ORDER_COMPLETED, order.getItem(),
                "Ai primit " + order.getAmount() + " RON pentru \"" + order.getItem().getName() + "\". " + resolution);
    }

    private void refund(PurchaseOrder order, String reason) {
        walletService.refund(order, reason);
        order.setStatus(Status.REFUNDED);
        order.setResolution("Rambursat: " + reason);
        order.setClosedAt(LocalDateTime.now());
        orderRepository.save(order);
        notificationService.notify(order.getBuyer(), Notification.Type.ORDER_RESOLVED, order.getItem(),
                "Ai primit inapoi " + order.getAmount() + " RON pentru \"" + order.getItem().getName() + "\": " + reason + ".");
    }

    private OrderView view(PurchaseOrder order, String viewer) {
        String role = order.getBuyer().getUsername().equals(viewer) ? "BUYER" : order.getSeller().getUsername().equals(viewer) ? "SELLER" : "ADMIN";
        return new OrderView(order.getId(), order.getItem().getId(), order.getItem().getName(), order.getItem().getImageUrl(),
                order.getBuyer().getUsername(), order.getSeller().getUsername(), order.getAmount(), order.getStatus(), order.getCourier(),
                order.getAwb(), order.getDisputeReason(), order.getResolution(), order.getCreatedAt(), order.getShippedAt(),
                order.getDisputedAt(), order.getClosedAt(),
                order.getStatus() == Status.PAID ? order.getCreatedAt().plusDays(SHIP_DAYS) : null,
                order.getStatus() == Status.SHIPPED ? order.getShippedAt().plusDays(CONFIRM_DAYS) : null, role);
    }

    private static boolean participant(PurchaseOrder order, String username) {
        return order.getBuyer().getUsername().equals(username) || order.getSeller().getUsername().equals(username);
    }

    private void requireSeller(PurchaseOrder order, String username) {
        if (!order.getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("Doar vanzatorul poate face asta.");
        }
    }

    private void requireBuyer(PurchaseOrder order, String username) {
        if (!order.getBuyer().getUsername().equals(username)) {
            throw new IllegalArgumentException("Doar cumparatorul poate face asta.");
        }
    }

    private PurchaseOrder find(Integer id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Comanda nu a fost gasita!"));
    }
}
