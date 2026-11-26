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
    private final OrderService orderService;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final AuctionScheduler auctionScheduler;
    private final ImageStorageService imageStorageService;
    private final AdminAccess adminAccess;
    private final ApplicationEventPublisher eventPublisher;
    private final com.DannyU.auction_engine.repository.ReportRepository reportRepository;

    public AdminDtos.Stats stats() {
        return new AdminDtos.Stats(
                userRepository.count(),
                itemRepository.count(),
                itemRepository.countByActiveTrue(),
                bidRepository.count(),
                itemRepository.countByWinnerIsNotNull(),
                itemRepository.sumSoldValue());
    }

    // Graficele: oferte pe zi (ultimele 14 zile, inclusiv zilele fara oferte), vanzari pe categorii, top 5 vanzatori
    public AdminDtos.Charts charts() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate first = today.minusDays(13);
        java.util.Map<java.time.LocalDate, Long> perDay = new java.util.HashMap<>();
        for (Object[] row : bidRepository.countPerDaySince(first.atStartOfDay())) {
            java.time.LocalDate day = row[0] instanceof java.sql.Date date ? date.toLocalDate() : (java.time.LocalDate) row[0];
            perDay.put(day, ((Number) row[1]).longValue());
        }
        List<AdminDtos.DayCount> days = first.datesUntil(today.plusDays(1))
                .map(day -> new AdminDtos.DayCount(day, perDay.getOrDefault(day, 0L)))
                .toList();

        List<AdminDtos.CategorySales> categories = itemRepository.salesByCategory().stream()
                .map(row -> {
                    com.DannyU.auction_engine.entity.Category c = (com.DannyU.auction_engine.entity.Category) row[0];
                    return new AdminDtos.CategorySales(c.name(), c.getLabel(), c.getIcon(), ((Number) row[1]).longValue(), (java.math.BigDecimal) row[2]);
                })
                .toList();

        List<AdminDtos.SellerSales> sellers = itemRepository.topSellers().stream()
                .limit(5)
                .map(row -> new AdminDtos.SellerSales((String) row[0], ((Number) row[1]).longValue(), (java.math.BigDecimal) row[2]))
                .toList();
        return new AdminDtos.Charts(days, categories, sellers);
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
                        adminAccess.isAdmin(user.getUsername()),
                        user.isBlocked()))
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
        // A sold item with money still in escrow: give it back to the buyer before the order disappears
        orderService.refundBeforeDeletion(itemId);
        bidRepository.deleteAllForItem(itemId);
        itemRepository.deleteById(itemId);
        itemRepository.flush();
        // Pozele se sterg abia dupa obiect (obiectul le referea printr-o cheie straina)
        images.forEach(imageStorageService::delete);
        eventPublisher.publishEvent(AuctionEvent.deleted(itemId));
    }

    // ---------- Reports and blocked accounts ----------

    public List<AdminDtos.ReportRow> openReports() {
        return reportRepository.findByStatusOrderByCreatedAtAsc(com.DannyU.auction_engine.entity.Report.Status.OPEN).stream()
                .map(r -> new AdminDtos.ReportRow(r.getId(), r.getItem().getId(), r.getItem().getName(),
                        r.getReporter() == null ? "Asistent AI" : r.getReporter().getUsername(),
                        r.getReason().name(), r.getDetails(), r.getCreatedAt(), r.getSource().name()))
                .toList();
    }

    @Transactional
    public void dismissReport(Integer reportId) {
        com.DannyU.auction_engine.entity.Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Raportarea nu a fost gasita!"));
        report.setStatus(com.DannyU.auction_engine.entity.Report.Status.DISMISSED);
        report.setResolvedAt(java.time.LocalDateTime.now());
    }

    @Transactional
    public void setBlocked(Integer userId, boolean blocked) {
        com.DannyU.auction_engine.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        if (blocked && adminAccess.isAdmin(user.getUsername())) {
            throw new IllegalArgumentException("Un administrator nu poate fi blocat!");
        }
        user.setBlocked(blocked);
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
