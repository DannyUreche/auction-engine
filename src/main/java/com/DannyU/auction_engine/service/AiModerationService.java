package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Report;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.ReportRepository;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Background AI moderation: every 90 s up to 5 new or edited listings are sent to Claude (ListingModerator);
// suspicious ones become an open report from "Asistent AI" in the admin's Raportari tab.
// Off without ANTHROPIC_API_KEY. A rate limit leaves the listing for the next run.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModerationService {

    static final int BATCH = 5;

    private final ListingModerator moderator;
    private final ItemRepository itemRepository;
    private final ReportRepository reportRepository;

    @Scheduled(fixedDelay = 90_000, initialDelay = 60_000)
    @Transactional
    public int checkNewListings() {
        if (!moderator.isEnabled()) {
            return 0;
        }
        List<Item> pending = itemRepository.findTop5ByAiCheckedFalseAndActiveTrueOrderByIdAsc();
        int flagged = 0;
        for (Item item : pending) {
            try {
                ListingModerator.Verdict verdict = moderator.review(item);
                if (verdict.suspicious() && !alreadyFlagged(item)) {
                    Report report = new Report();
                    report.setItem(item);
                    report.setSource(Report.Source.AI);
                    report.setReason(ListingModerator.reasonOf(verdict));
                    report.setDetails(verdict.explanation() == null ? null
                            : verdict.explanation().substring(0, Math.min(500, verdict.explanation().length())));
                    reportRepository.save(report);
                    flagged++;
                }
                item.setAiChecked(true);
            } catch (RateLimitException e) {
                log.info("AI moderation rate limited, will retry");
                break;
            } catch (AnthropicServiceException e) {
                log.warn("AI moderation failed for item {}: {}", item.getId(), e.getMessage());
                item.setAiChecked(true);
            }
            itemRepository.save(item);
        }
        return flagged;
    }

    private boolean alreadyFlagged(Item item) {
        return reportRepository.existsByItemIdAndSourceAndStatus(item.getId(), Report.Source.AI, Report.Status.OPEN);
    }
}
