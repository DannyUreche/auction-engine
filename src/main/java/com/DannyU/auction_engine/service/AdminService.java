package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.dto.AdminDtos;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Operatiile din pagina de administrare
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final AuctionScheduler auctionScheduler;
    private final ImageStorageService imageStorageService;
    private final AdminAccess adminAccess;
    private final ApplicationEventPublisher eventPublisher;

    public AdminDtos.Stats stats() {
        return new AdminDtos.Stats(
                userRepository.count(),
                itemRepository.count(),
                itemRepository.countByActiveTrue(),
                bidRepository.count(),
                itemRepository.countByWinnerIsNotNull(),
                itemRepository.sumSoldValue());
    }

    public List<AdminDtos.UserRow> users() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(user -> new AdminDtos.UserRow(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getCreatedAt(),
                        itemRepository.countBySellerId(user.getId()),
                        bidRepository.countByBidderId(user.getId()),
                        adminAccess.isAdmin(user.getUsername())))
                .toList();
    }

    public List<AdminDtos.ItemRow> items() {
        return itemRepository.findAllByOrderByIdDesc().stream().map(this::toRow).toList();
    }

    // Inchide licitatia imediat (castigatorul este cel cu oferta cea mai mare in acest moment)
    @Transactional
    public AdminDtos.ItemRow closeNow(Integer itemId) {
        Item item = findItem(itemId);
        if (!item.getActive()) {
            throw new IllegalArgumentException("Licitatia este deja inchisa!");
        }
        auctionScheduler.closeAuction(item);
        return toRow(item);
    }

    // Sterge obiectul impreuna cu ofertele si pozele lui (urmaririle se sterg singure: ON DELETE CASCADE)
    @Transactional
    public void deleteItem(Integer itemId) {
        List<String> images = List.copyOf(findItem(itemId).getImageFilenames());
        bidRepository.deleteAllForItem(itemId);
        itemRepository.deleteById(itemId);
        itemRepository.flush();
        // Pozele se sterg abia dupa obiect (obiectul le referea printr-o cheie straina)
        images.forEach(imageStorageService::delete);
        eventPublisher.publishEvent(AuctionEvent.deleted(itemId));
    }

    private Item findItem(Integer itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
    }

    private AdminDtos.ItemRow toRow(Item item) {
        return new AdminDtos.ItemRow(
                item.getId(),
                item.getName(),
                item.getImageUrl(),
                item.getSeller().getUsername(),
                item.getStartPrice(),
                item.getCurrentPrice(),
                item.getActive(),
                item.getEndTime(),
                bidRepository.countByItemId(item.getId()),
                item.getWinner() == null ? null : item.getWinner().getUsername());
    }
}
