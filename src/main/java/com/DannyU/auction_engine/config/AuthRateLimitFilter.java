package com.DannyU.auction_engine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Limita de cereri pentru login, inregistrare si "am uitat parola": cel mult N pe minut de pe aceeasi adresa IP.
// Altfel cineva ar putea incerca mii de parole pe secunda. Peste limita raspundem 429 cu {"eroare": ...}.
// Pastram in memorie momentele ultimelor cereri (fereastra glisanta de 60 de secunde) pentru fiecare IP.
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final Set<String> LIMITED = Set.of("/api/auth/login", "/api/auth/register", "/api/auth/forgot-password",
            "/api/auth/resend-verification", "/api/auth/google");

    private final int maxPerMinute;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(@Value("${app.rate-limit.auth-per-minute:10}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        Deque<Long> times = requests.computeIfAbsent(clientIp(request), ip -> new ArrayDeque<>());
        long retryAfterSeconds;
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() < maxPerMinute) {
                times.addLast(now);
                retryAfterSeconds = 0;
            } else {
                retryAfterSeconds = Math.max(1, (WINDOW_MS - (now - times.peekFirst())) / 1000);
            }
        }
        if (retryAfterSeconds == 0) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"eroare\":\"Prea multe incercari! Mai incearca peste " + retryAfterSeconds + " secunde.\"}");
    }

    // Pe Render cererile trec printr-un proxy: adresa reala a clientului e prima din X-Forwarded-For
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    // Curatenie: IP-urile fara cereri recente nu mai ocupa memorie
    @Scheduled(fixedRate = 300_000)
    public void forgetOldClients() {
        long now = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().isEmpty() || now - entry.getValue().peekLast() > WINDOW_MS;
            }
        });
    }
}
