package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Review;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Recenzii intre cumparator si vanzator, dupa ce obiectul s-a vandut
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ItemRepository itemRepository;

    // Recenzia asa cum apare in pagini (fara emailuri). role = rolul autorului in vanzare
    public record ReviewDto(Integer id, int rating, String comment, String author, String target, String role,
                            Integer itemId, String itemName, LocalDateTime createdAt) {

        static ReviewDto of(Review r) {
            boolean byWinner = r.getItem().getWinner() != null && r.getItem().getWinner().getId().equals(r.getAuthor().getId());
            return new ReviewDto(r.getId(), r.getRating(), r.getComment(), r.getAuthor().getUsername(),
                    r.getTarget().getUsername(), byWinner ? "cumparator" : "vanzator",
                    r.getItem().getId(), r.getItem().getName(), r.getCreatedAt());
        }
    }

    public record Rating(double average, long count) {
    }

    @Transactional
    public ReviewDto create(Integer itemId, String username, int rating, String comment) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (item.getActive() || item.getWinner() == null) {
            throw new IllegalArgumentException("Poti lasa o recenzie doar dupa ce obiectul a fost vandut!");
        }
        User author;
        User target;
        if (item.getSeller().getUsername().equals(username)) {
            author = item.getSeller();
            target = item.getWinner();
        } else if (item.getWinner().getUsername().equals(username)) {
            author = item.getWinner();
            target = item.getSeller();
        } else {
            throw new IllegalArgumentException("Doar vanzatorul si castigatorul pot lasa recenzii pentru acest obiect!");
        }
        if (reviewRepository.existsByItemIdAndAuthorUsername(itemId, username)) {
            throw new IllegalArgumentException("Ai lasat deja o recenzie pentru acest obiect!");
        }

        Review review = new Review();
        review.setItem(item);
        review.setAuthor(author);
        review.setTarget(target);
        review.setRating((short) rating);
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        return ReviewDto.of(reviewRepository.save(review));
    }

    public List<ReviewDto> forItem(Integer itemId) {
        return reviewRepository.findByItemIdOrderByCreatedAtAsc(itemId).stream().map(ReviewDto::of).toList();
    }

    public List<ReviewDto> receivedBy(String username) {
        return reviewRepository.findTop20ByTargetUsernameOrderByCreatedAtDesc(username).stream().map(ReviewDto::of).toList();
    }

    public Rating ratingOf(String username) {
        return new Rating(Math.round(reviewRepository.averageFor(username) * 10) / 10.0,
                reviewRepository.countByTargetUsername(username));
    }
}
