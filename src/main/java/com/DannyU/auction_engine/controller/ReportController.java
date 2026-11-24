package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Report;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.ReportRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Reporting a suspicious item (admins see the reports in the admin page)
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public record ReportRequest(
            @NotNull(message = "Alege motivul raportarii!") Report.Reason reason,
            @Size(max = 500, message = "Detaliile pot avea maxim 500 de caractere!") String details) {
    }

    public record MessageResponse(String mesaj) {
    }

    @PostMapping("/api/items/{id}/report")
    public MessageResponse report(@PathVariable Integer id, @Valid @RequestBody ReportRequest request, Authentication authentication) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        String username = authentication.getName();
        if (item.getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("Nu poti raporta propriul obiect!");
        }
        if (reportRepository.existsByItemIdAndReporterUsername(id, username)) {
            throw new IllegalArgumentException("Ai raportat deja acest obiect!");
        }
        Report report = new Report();
        report.setItem(item);
        report.setReporter(userRepository.findByUsername(username).orElseThrow());
        report.setReason(request.reason());
        report.setDetails(request.details() == null || request.details().isBlank() ? null : request.details().trim());
        reportRepository.save(report);
        return new MessageResponse("Multumim! Un administrator va verifica obiectul.");
    }
}
