package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuditEntry;
import com.DannyU.auction_engine.repository.AuditEntryRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.ReportRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Audit log of admin actions. AdminController records each action after it succeeded; labels (item name,
// username) are read before the action, so deleted objects stay readable in the log.
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public void record(String admin, AuditEntry.Action action, String targetType, Integer targetId, String label, String details) {
        AuditEntry entry = new AuditEntry();
        entry.setAdmin(admin);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setTargetLabel(truncate(label, 200));
        entry.setDetails(truncate(details, 300));
        auditEntryRepository.save(entry);
    }

    public List<AuditEntry> recent() {
        return auditEntryRepository.findTop200ByOrderByCreatedAtDescIdDesc();
    }

    public String itemLabel(Integer itemId) {
        return itemRepository.findById(itemId).map(item -> item.getName()).orElse("#" + itemId);
    }

    public String userLabel(Integer userId) {
        return userRepository.findById(userId).map(user -> user.getUsername()).orElse("#" + userId);
    }

    public String reportLabel(Integer reportId) {
        return reportRepository.findById(reportId)
                .map(report -> report.getItem().getName() + " (" + report.getReason().name() + ")")
                .orElse("#" + reportId);
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
