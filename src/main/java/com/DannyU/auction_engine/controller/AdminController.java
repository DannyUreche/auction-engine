package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.dto.AdminDtos;
import com.DannyU.auction_engine.service.AdminService;
import com.DannyU.auction_engine.service.DemoDataService;
import com.DannyU.auction_engine.service.EmailService;
import com.DannyU.auction_engine.repository.UserRepository;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Rutele de administrare. Accesul (doar rolul ADMIN) este verificat in SecurityConfig.
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DemoDataService demoDataService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final com.DannyU.auction_engine.service.ClaudeClient claudeClient;
    private final com.DannyU.auction_engine.repository.ItemRepository itemRepository;
    private final com.DannyU.auction_engine.service.FeaturedService featuredService;

    public record EmailStatus(boolean configured, String from) {
    }

    public record Message(String mesaj) {
    }

    public record AiStatus(boolean enabled, long pendingListings) {
    }

    // AI features on? (ANTHROPIC_API_KEY) and how many listings still wait for the AI moderation
    @GetMapping("/ai")
    public AiStatus aiStatus() {
        return new AiStatus(claudeClient.isEnabled(), itemRepository.countByAiCheckedFalseAndActiveTrue());
    }

    // Is SMTP configured? (the admin panel shows it next to the "send test email" button)
    @GetMapping("/email")
    public EmailStatus emailStatus() {
        return new EmailStatus(emailService.isConfigured(), emailService.isConfigured() ? emailService.from() : null);
    }

    // Sends a test email to the logged-in admin, to check the SMTP settings (e.g. Brevo)
    @PostMapping("/email/test")
    public Message sendTestEmail(Authentication authentication) {
        String to = userRepository.findByUsername(authentication.getName()).orElseThrow().getEmail();
        emailService.sendOrThrow(to, "Test Licitatii.live",
                "Salut!\n\nAcesta este un email de test trimis din pagina de administrare.\n"
                        + "Daca il primesti, notificarile pe email functioneaza.\n\n" + emailService.link("/"));
        return new Message("Emailul de test a fost trimis la " + to + ". Verifica si folderul Spam.");
    }

    public record DemoStatus(boolean exists, int items) {
    }

    // Date demo pentru prezentari (utilizatori demo_..., obiecte cu poze si oferte)
    @GetMapping("/demo-data")
    public DemoStatus demoStatus() {
        return new DemoStatus(demoDataService.exists(), 0);
    }

    @PostMapping("/demo-data")
    public DemoStatus generateDemoData() {
        return new DemoStatus(true, demoDataService.generate());
    }

    @DeleteMapping("/demo-data")
    public DemoStatus deleteDemoData() {
        return new DemoStatus(false, demoDataService.delete());
    }

    // Choose / reset "Licitatia saptamanii" (without a choice the busiest auction is shown)
    @PostMapping("/items/{id}/feature")
    public ResponseEntity<Void> feature(@PathVariable Integer id) {
        featuredService.choose(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/featured")
    public ResponseEntity<Void> clearFeatured() {
        featuredService.clear();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public AdminDtos.Stats stats() {
        return adminService.stats();
    }

    @GetMapping("/charts")
    public AdminDtos.Charts charts() {
        return adminService.charts();
    }

    // Reports of suspicious items (deleting the item removes its reports too)
    @GetMapping("/reports")
    public List<AdminDtos.ReportRow> reports() {
        return adminService.openReports();
    }

    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<Void> dismissReport(@PathVariable Integer id) {
        adminService.dismissReport(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<Void> block(@PathVariable Integer id) {
        adminService.setBlocked(id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable Integer id) {
        adminService.setBlocked(id, false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public List<AdminDtos.UserRow> users() {
        return adminService.users();
    }

    @GetMapping("/items")
    public List<AdminDtos.ItemRow> items() {
        return adminService.items();
    }

    @PostMapping("/items/{id}/close")
    public AdminDtos.ItemRow closeNow(@PathVariable Integer id) {
        return adminService.closeNow(id);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id) {
        adminService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
