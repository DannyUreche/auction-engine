package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.PurchaseOrder;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.entity.WalletTransaction;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Virtual wallet. The balance changes on top-ups and on orders: the winner pays when the auction closes, the
// seller is paid when the order completes (escrow, see OrderService);
// while you lead an auction its current price is "reserved", so you cannot bid more money than you have.
@Service
@RequiredArgsConstructor
public class WalletService {

    public static final BigDecimal MAX_TOP_UP = new BigDecimal("5000");

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final WalletTransactionRepository transactionRepository;
    private final com.DannyU.auction_engine.repository.PurchaseOrderRepository orderRepository;
    private final jakarta.persistence.EntityManager entityManager;

    // inEscrow: what I paid for orders not yet completed; pendingSales: what I will receive when buyers confirm
    public record Wallet(BigDecimal balance, BigDecimal reserved, BigDecimal available, BigDecimal inEscrow,
                         BigDecimal pendingSales, List<WalletTransaction> transactions) {
    }

    public Wallet wallet(String username) {
        User user = findUser(username);
        BigDecimal reserved = itemRepository.reservedBy(username);
        return new Wallet(user.getBalance(), reserved, user.getBalance().subtract(reserved), orderRepository.heldFor(username),
                orderRepository.pendingFor(username), transactionRepository.findTop30ByUserUsernameOrderByCreatedAtDescIdDesc(username));
    }

    // Money the user can still commit to this item (what they already reserve on it counts as theirs)
    public BigDecimal availableFor(User user, Item item) {
        BigDecimal reserved = itemRepository.reservedBy(user.getUsername());
        if (item.getAuctionType() == AuctionType.ENGLISH && leads(user, item)) {
            reserved = reserved.subtract(item.getCurrentPrice());
        }
        return user.getBalance().subtract(reserved);
    }

    public void checkFunds(User user, Item item, BigDecimal amount) {
        BigDecimal available = availableFor(user, item);
        if (available.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Fonduri insuficiente! Ai disponibil " + available.max(BigDecimal.ZERO)
                    + " RON. Alimenteaza portofelul din Contul meu.");
        }
    }

    // The auction closed with a winner: the price leaves the buyer's wallet and waits in escrow (OrderService)
    @Transactional
    public void hold(Item item) {
        User winner = lockForUpdate(item.getWinner());
        winner.setBalance(winner.getBalance().subtract(item.getCurrentPrice()));
        userRepository.save(winner);
        record(winner, WalletTransaction.Type.PURCHASE, item.getCurrentPrice().negate(), item,
                "Ai platit \"" + item.getName() + "\" - banii stau in siguranta pana confirmi primirea");
    }

    // Escrow released: the seller gets the money
    @Transactional
    public void release(PurchaseOrder order) {
        User seller = lockForUpdate(order.getSeller());
        seller.setBalance(seller.getBalance().add(order.getAmount()));
        userRepository.save(seller);
        record(seller, WalletTransaction.Type.SALE, order.getAmount(), order.getItem(),
                "Ai incasat \"" + order.getItem().getName() + "\" de la " + order.getBuyer().getUsername());
    }

    // Escrow refunded: the money goes back to the buyer
    @Transactional
    public void refund(PurchaseOrder order, String reason) {
        User buyer = lockForUpdate(order.getBuyer());
        buyer.setBalance(buyer.getBalance().add(order.getAmount()));
        userRepository.save(buyer);
        record(buyer, WalletTransaction.Type.REFUND, order.getAmount(), order.getItem(),
                "Banii pentru \"" + order.getItem().getName() + "\" ti-au fost returnati: " + reason);
    }

    // Re-read the user with a row lock, so two balance changes at the same time can't overwrite each other
    private User lockForUpdate(User user) {
        if (entityManager.contains(user)) {
            entityManager.flush();
            entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            return user;
        }
        User managed = entityManager.find(User.class, user.getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return managed;
    }

    @Transactional
    public Wallet topUp(String username, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(MAX_TOP_UP) > 0) {
            throw new IllegalArgumentException("Poti alimenta intre 1 si " + MAX_TOP_UP + " RON odata!");
        }
        credit(findUser(username), amount, "Alimentare portofel (credite de test)");
        return wallet(username);
    }

    // Adds money to a wallet (test credits or a card payment) under a row lock
    @Transactional
    public void credit(User user, BigDecimal amount, String description) {
        User locked = lockForUpdate(user);
        locked.setBalance(locked.getBalance().add(amount));
        userRepository.save(locked);
        record(locked, WalletTransaction.Type.TOP_UP, amount, null, description);
    }

    private boolean leads(User user, Item item) {
        return bidRepository.findTopByItemIdOrderByAmountDesc(item.getId())
                .map(bid -> bid.getBidder().getId().equals(user.getId()))
                .orElse(false);
    }

    private void record(User user, WalletTransaction.Type type, BigDecimal amount, Item item, String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setItemId(item == null ? null : item.getId());
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
    }
}
