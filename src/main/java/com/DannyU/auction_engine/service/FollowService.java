package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.SellerFollow;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.SellerFollowRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Following sellers. The notification for a new listing is sent by AlertService.auctionOpened.
@Service
@RequiredArgsConstructor
public class FollowService {

    private final SellerFollowRepository sellerFollowRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public record FollowStatus(boolean following, long followers) {
    }

    public record FollowedSeller(String username, long activeItems, long followers) {
    }

    @Transactional(readOnly = true)
    public FollowStatus status(String follower, String seller) {
        return new FollowStatus(sellerFollowRepository.findByFollower_UsernameAndSeller_Username(follower, seller).isPresent(),
                sellerFollowRepository.countBySeller_Username(seller));
    }

    public long followers(String seller) {
        return sellerFollowRepository.countBySeller_Username(seller);
    }

    // Idempotent: following twice is fine
    @Transactional
    public FollowStatus follow(String follower, String seller) {
        if (follower.equals(seller)) {
            throw new IllegalArgumentException("Nu te poti urmari pe tine insuti!");
        }
        if (sellerFollowRepository.findByFollower_UsernameAndSeller_Username(follower, seller).isEmpty()) {
            SellerFollow follow = new SellerFollow();
            follow.setFollower(find(follower));
            follow.setSeller(find(seller));
            sellerFollowRepository.save(follow);
        }
        return status(follower, seller);
    }

    @Transactional
    public FollowStatus unfollow(String follower, String seller) {
        sellerFollowRepository.findByFollower_UsernameAndSeller_Username(follower, seller).ifPresent(sellerFollowRepository::delete);
        return status(follower, seller);
    }

    @Transactional(readOnly = true)
    public List<FollowedSeller> following(String follower) {
        return sellerFollowRepository.findByFollower_UsernameOrderByCreatedAtDesc(follower).stream()
                .map(follow -> follow.getSeller().getUsername())
                .map(seller -> new FollowedSeller(seller,
                        itemRepository.findBySellerUsernameOrderByIdDesc(seller).stream().filter(item -> Boolean.TRUE.equals(item.getActive())).count(),
                        followers(seller)))
                .toList();
    }

    private User find(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
    }
}
