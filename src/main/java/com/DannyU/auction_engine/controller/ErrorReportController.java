package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.AppError;
import com.DannyU.auction_engine.entity.AuditEntry;
import com.DannyU.auction_engine.service.ErrorMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Error monitoring: browsers report JavaScript errors here (public); admins list and resolve them
@RestController
@RequiredArgsConstructor
public class ErrorReportController {

    private final ErrorMonitorService errorMonitorService;
    private final com.DannyU.auction_engine.service.AuditService auditService;

    public record ClientError(String message, String stack, String url) {
    }

    @PostMapping("/api/client-errors")
    public ResponseEntity<Void> report(@RequestBody ClientError error, Authentication authentication, HttpServletRequest request) {
        String username = authentication == null || authentication instanceof AnonymousAuthenticationToken ? null : authentication.getName();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        errorMonitorService.recordClientError(error.message(), error.stack(), error.url(), username, request.getHeader("User-Agent"), ip);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/errors")
    public List<AppError> list() {
        return errorMonitorService.list();
    }

    @PostMapping("/api/admin/errors/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Integer id, Authentication authentication) {
        errorMonitorService.resolve(id);
        auditService.record(authentication.getName(), AuditEntry.Action.RESOLVE_ERROR, "ERROR", id, null, null);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/errors/resolved")
    public Map<String, Integer> deleteResolved(Authentication authentication) {
        int deleted = errorMonitorService.deleteResolved();
        auditService.record(authentication.getName(), AuditEntry.Action.DELETE_RESOLVED_ERRORS, null, null, null, deleted + " erori");
        return Map.of("sterse", deleted);
    }
}
