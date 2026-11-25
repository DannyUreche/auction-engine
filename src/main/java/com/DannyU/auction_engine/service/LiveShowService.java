package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.LiveShow;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.SellerFollow;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.LiveShowRepository;
import com.DannyU.auction_engine.repository.SellerFollowRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

// Live show: a seller puts 2-10 of their items "on stage"; item i is auctioned between start + i*slot and
// start + (i+1)*slot. It reuses scheduled auctions (Item.startTime/endTime), so bidding, anti-sniping, closing and
// the STARTED notifications work as for any other auction. Followers are told about the show when it is created.
@Service
@RequiredArgsConstructor
public class LiveShowService {

    static final int MIN_ITEMS = 2;
    static final int MAX_ITEMS = 10;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final LiveShowRepository liveShowRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final SellerFollowRepository sellerFollowRepository;
    private final NotificationService notificationService;

    public enum Status { SCHEDULED, LIVE, ENDED }

    public record ShowSummary(Integer id, String title, String sellerUsername, LocalDateTime startTime, LocalDateTime endTime,
                              int slotSeconds, Status status, int items, String coverUrl) {
    }

    public record ShowDetails(ShowSummary show, List<Item> items) {
    }

    // The seller's items that can go on stage: active classic auctions without bids, not in another show
    public List<Item> eligibleItems(String username) {
        return itemRepository.findBySellerUsernameOrderByIdDesc(username).stream().filter(this::eligible).toList();
    }

    @Transactional
    public ShowDetails create(String username, String title, LocalDateTime start, int slotMinutes, List<Integer> itemIds) {
        User seller = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(itemIds == null ? List.of() : itemIds));
        if (ids.size() < MIN_ITEMS || ids.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Alege intre " + MIN_ITEMS + " si " + MAX_ITEMS + " obiecte pentru licitatia live!");
        }
        if (slotMinutes < 1 || slotMinutes > 15) {
            throw new IllegalArgumentException("Fiecare obiect poate fi licitat intre 1 si 15 minute!");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = start == null ? now.plusMinutes(1) : start;
        if (startTime.isBefore(now.minusSeconds(30)) || startTime.isAfter(now.plusDays(14))) {
            throw new IllegalArgumentException("Licitatia live poate incepe acum sau in urmatoarele 14 zile!");
        }

        List<Item> items = new ArrayList<>();
        for (Integer id : ids) {
            Item item = itemRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
            if (!item.getSeller().getId().equals(seller.getId())) {
                throw new IllegalArgumentException("Poti pune pe scena doar obiectele tale!");
            }
            if (!eligible(item)) {
                throw new IllegalArgumentException("\"" + item.getName() + "\" nu poate intra in licitatia live (are oferte, e olandeza, s-a incheiat sau e deja intr-o alta licitatie live).");
            }
            items.add(item);
        }

        LiveShow show = new LiveShow();
        show.setSeller(seller);
        show.setTitle(title == null || title.isBlank() ? "Licitatia live a lui " + seller.getUsername() : title.trim());
        show.setStartTime(startTime);
        show.setSlotSeconds(slotMinutes * 60);
        liveShowRepository.save(show);

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            LocalDateTime slotStart = startTime.plusSeconds((long) i * show.getSlotSeconds());
            item.setLiveShow(show);
            item.setShowPosition(i);
            item.setStartTime(slotStart);
            item.setEndTime(slotStart.plusSeconds(show.getSlotSeconds()));
            item.setStartNotified(!slotStart.isAfter(now));
            itemRepository.save(item);
        }

        String message = seller.getUsername() + " organizeaza o licitatie live: \"" + show.getTitle() + "\" (" + items.size()
                + " obiecte, de la " + startTime.format(WHEN) + " UTC)";
        for (SellerFollow follow : sellerFollowRepository.findBySeller_Id(seller.getId())) {
            notificationService.notify(follow.getFollower(), Notification.Type.LIVE_SHOW, items.getFirst(), message);
        }
        return new ShowDetails(summary(show, items), items);
    }

    @Transactional(readOnly = true)
    public ShowDetails details(Integer id) {
        LiveShow show = liveShowRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Licitatia live nu a fost gasita!"));
        List<Item> items = itemRepository.findByLiveShow_IdOrderByShowPositionAsc(id);
        return new ShowDetails(summary(show, items), items);
    }

    // Running and upcoming shows (ended ones only from the last 6 hours), live first
    @Transactional(readOnly = true)
    public List<ShowSummary> list() {
        LocalDateTime now = LocalDateTime.now();
        return liveShowRepository.findByStartTimeAfterOrderByStartTimeAsc(now.minusDays(1)).stream()
                .map(show -> summary(show, itemRepository.findByLiveShow_IdOrderByShowPositionAsc(show.getId())))
                .filter(summary -> summary.status() != Status.ENDED || summary.endTime().isAfter(now.minusHours(6)))
                .sorted((a, b) -> Integer.compare(a.status().ordinal() == 1 ? 0 : 1, b.status().ordinal() == 1 ? 0 : 1))
                .toList();
    }

    private ShowSummary summary(LiveShow show, List<Item> items) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = items.stream().map(Item::getEndTime).max(LocalDateTime::compareTo).orElse(show.getStartTime());
        boolean anyActive = items.stream().anyMatch(item -> Boolean.TRUE.equals(item.getActive()));
        Status status = now.isBefore(show.getStartTime()) ? Status.SCHEDULED : anyActive ? Status.LIVE : Status.ENDED;
        String cover = items.stream().map(Item::getImageUrl).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return new ShowSummary(show.getId(), show.getTitle(), show.getSellerUsername(), show.getStartTime(), end,
                show.getSlotSeconds(), status, items.size(), cover);
    }

    private boolean eligible(Item item) {
        return Boolean.TRUE.equals(item.getActive()) && item.getAuctionType() == AuctionType.ENGLISH && item.getLiveShow() == null
                && bidRepository.countByItemId(item.getId()) == 0;
    }
}
