package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Bids at the same moment: the item row is locked, so of 8 identical bids exactly one wins and the others are
// rejected as too low. Not @Transactional: every thread commits its own transaction, like real requests.
@SpringBootTest
class ConcurrentBidTest {

    @Autowired
    private BidService bidService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactions;

    private final List<User> users = new ArrayList<>();
    private Item item;

    @Test
    void onlyOneOfManyIdenticalBidsWins() throws Exception {
        User seller = user("vanzator_concurent");
        item = new Item();
        item.setName("Obiect disputat");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setSeller(seller);
        item.setCategory(Category.ALTELE);
        item.setEndTime(LocalDateTime.now().plusHours(1));
        item = itemRepository.save(item);

        int bidders = 8;
        List<User> buyers = new ArrayList<>();
        for (int i = 0; i < bidders; i++) {
            buyers.add(user("licitator_concurent"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(bidders);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (User buyer : buyers) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    bidService.placeBid(item.getId(), buyer.getUsername(), new BigDecimal("105"));
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }));
        }
        start.countDown(); // all threads bid at once
        int accepted = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                accepted++;
            }
        }
        pool.shutdown();

        assertThat(accepted).isEqualTo(1);
        assertThat(bidRepository.countByItemId(item.getId())).isEqualTo(1);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getCurrentPrice()).isEqualByComparingTo("105");
    }

    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            if (item != null) {
                bidRepository.deleteAllForItem(item.getId());
                itemRepository.deleteById(item.getId());
            }
            userRepository.deleteAll(users);
        });
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        user = userRepository.save(user);
        users.add(user);
        return user;
    }
}
