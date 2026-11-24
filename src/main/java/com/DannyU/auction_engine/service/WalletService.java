package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.Item;
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

// Virtual wallet. The balance only changes on top-ups and when an auction closes (winner pays the seller);
// while you lead an auction its current price is "reserved", so you cannot bid more money than you have.
@Service
@RequiredArgsConstructor
public class WalletService {

    public static final BigDecimal MAX_TOP_UP = new BigDecimal("5000");

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final WalletTransactionRepository transactionRepository;

    public record Wallet(BigDecimal balance, BigDecimal reserved, BigDecimal available, List<WalletTransaction> transactions) {
    }

    public Wallet wallet(String username) {
        User user = findUser(username);
        BigDecimal reserved = itemRepository.reservedBy(username);
        return new Wallet(user.getBalance(), reserved, user.getBalance().subtract(reserved),
                transactionRepository.findTop30ByUserUsernameOrderByCreatedAtDescIdDesc(username));
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

    // The auction closed with a winner: the winner pays the seller
    @Transactional
    public void settle(Item item) {
        User winner = item.getWinner();
        User seller = item.getSeller();
        BigDecimal price = item.getCurrentPrice();
        winner.setBalance(winner.getBalance().subtract(price));
        seller.setBalance(seller.getBalance().add(price));
        userRepository.save(winner);
        userRepository.save(seller);
        record(winner, WalletTransaction.Type.PURCHASE, price.negate(), item, "Ai cumparat \"" + item.getName() + "\"");
        record(seller, WalletTransaction.Type.SALE, price, item, "Ai vandut \"" + item.getName() + "\" lui " + winner.getUsername());
    }

    @Transactional
    public Wallet topUp(String username, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(MAX_TOP_UP) > 0) {
            throw new IllegalArgumentException("Poti alimenta intre 1 si " + MAX_TOP_UP + " RON odata!");
        }
        User user = findUser(username);
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
        record(user, WalletTransaction.Type.TOP_UP, amount, null, "Alimentare portofel (credite de test)");
        return wallet(username);
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
