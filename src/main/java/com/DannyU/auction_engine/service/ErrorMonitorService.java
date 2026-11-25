package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AppError;
import com.DannyU.auction_engine.repository.AppErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Self-hosted error monitoring (instead of an external service such as Sentry): unexpected server errors and
// JavaScript errors from browsers are stored grouped by fingerprint, with a counter, and listed in the admin page.
// Recording never throws: monitoring must not break the request that failed.
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorMonitorService {

    static final int MAX_OPEN_ERRORS = 300;
    static final int CLIENT_REPORTS_PER_MINUTE = 20;

    private final AppErrorRepository appErrorRepository;
    private final Map<String, Deque<Long>> clientReports = new ConcurrentHashMap<>();

    // REQUIRES_NEW: the error is saved even when the failed request's transaction rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordServerError(Throwable error, String path, String username, String userAgent) {
        try {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            String message = error.getClass().getSimpleName() + ": " + error.getMessage();
            String firstFrame = error.getStackTrace().length > 0 ? error.getStackTrace()[0].toString() : "";
            save(AppError.Source.SERVER, message, trace.toString(), firstFrame, path, username, userAgent);
        } catch (RuntimeException e) {
            log.warn("Could not record server error", e);
        }
    }

    @Transactional
    public void recordClientError(String message, String stack, String url, String username, String userAgent, String clientIp) {
        if (message == null || message.isBlank() || !allowClientReport(clientIp)) {
            return;
        }
        String firstLine = stack == null ? "" : stack.lines().skip(1).findFirst().orElse("").trim();
        save(AppError.Source.CLIENT, message.trim(), stack, firstLine, url, username, userAgent);
    }

    public List<AppError> list() {
        return appErrorRepository.findTop100ByOrderByResolvedAscLastSeenDesc();
    }

    public long openCount() {
        return appErrorRepository.countByResolvedFalse();
    }

    @Transactional
    public void resolve(Integer id) {
        AppError error = appErrorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Eroarea nu a fost gasita!"));
        error.setResolved(true);
    }

    @Transactional
    public int deleteResolved() {
        return appErrorRepository.deleteResolved();
    }

    private void save(AppError.Source source, String message, String details, String firstFrame, String path,
                      String username, String userAgent) {
        String fingerprint = sha256(source + "|" + truncate(message, 300) + "|" + firstFrame);
        AppError error = appErrorRepository.findFirstByFingerprintAndResolvedFalse(fingerprint).orElse(null);
        if (error != null) {
            error.setOccurrences(error.getOccurrences() + 1);
            error.setLastSeen(LocalDateTime.now());
            error.setPath(truncate(path, 300));
            error.setUsername(truncate(username, 50));
            appErrorRepository.save(error);
            return;
        }
        if (appErrorRepository.countByResolvedFalse() >= MAX_OPEN_ERRORS) {
            return; // something is flooding the monitor; the existing rows already show it
        }
        error = new AppError();
        error.setFingerprint(fingerprint);
        error.setSource(source);
        error.setMessage(truncate(message, 2000));
        error.setDetails(truncate(details, 8000));
        error.setPath(truncate(path, 300));
        error.setUsername(truncate(username, 50));
        error.setUserAgent(truncate(userAgent, 300));
        appErrorRepository.save(error);
    }

    // At most CLIENT_REPORTS_PER_MINUTE reports per IP (the endpoint is public)
    private boolean allowClientReport(String clientIp) {
        long now = System.currentTimeMillis();
        Deque<Long> times = clientReports.computeIfAbsent(clientIp == null ? "?" : clientIp, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > 60_000) {
                times.pollFirst();
            }
            if (times.size() >= CLIENT_REPORTS_PER_MINUTE) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    private static String truncate(String text, int max) {
        return text == null ? null : text.length() > max ? text.substring(0, max) : text;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
