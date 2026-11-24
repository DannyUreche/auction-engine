package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.WatchedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// "Te-ar putea interesa": running auctions from the categories the user bids on or watches,
// then the most popular ones; never their own items or items they already bid on / watch.
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int LIMIT = 6;

    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final WatchedItemRepository watchedItemRepository;

    public List<Item> recommendedFor(String username) {
        Set<Category> categories = new HashSet<>();
        Set<Integer> seen = new HashSet<>();
        for (Bid bid : bidRepository.findByBidderUsernameOrderByCreatedAtDesc(username)) {
            categories.add(bid.getItem().getCategory());
            seen.add(bid.getItem().getId());
        }
        for (Item item : watchedItemRepository.findItemsWatchedBy(username)) {
            categories.add(item.getCategory());
            seen.add(item.getId());
        }

        List<Item> candidates = itemRepository.findByActiveTrueOrderByEndTimeAsc().stream()
                .filter(item -> !item.getSeller().getUsername().equals(username))
                .filter(item -> !seen.contains(item.getId()))
                .toList();
        Stream<Item> preferred = candidates.stream().filter(item -> categories.contains(item.getCategory()));
        Stream<Item> popular = candidates.stream()
                .filter(item -> !categories.contains(item.getCategory()))
                .sorted(Comparator.comparingLong((Item item) -> bidRepository.countByItemId(item.getId())).reversed());
        return Stream.concat(preferred, popular).limit(LIMIT).toList();
    }
}
