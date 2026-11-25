package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.dto.AdminDtos;
import com.DannyU.auction_engine.entity.AuditEntry;
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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Administrare", description = "Doar pentru administratori (rol ADMIN).")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DemoDataService demoDataService;
    private final com.DannyU.auction_engine.service.AuditService auditService;
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
    @io.swagger.v3.oas.annotations.Operation(summary = "Starea moderarii AI")
    @GetMapping("/ai")
    public AiStatus aiStatus() {
        return new AiStatus(claudeClient.isEnabled(), itemRepository.countByAiCheckedFalseAndActiveTrue());
    }

    // Is SMTP configured? (the admin panel shows it next to the "send test email" button)
    @io.swagger.v3.oas.annotations.Operation(summary = "Starea trimiterii de emailuri")
    @GetMapping("/email")
    public EmailStatus emailStatus() {
        return new EmailStatus(emailService.isConfigured(), emailService.isConfigured() ? emailService.from() : null);
    }

    // Sends a test email to the logged-in admin, to check the SMTP settings (e.g. Brevo)
    @io.swagger.v3.oas.annotations.Operation(summary = "Trimite un email de test")
    @PostMapping("/email/test")
    public Message sendTestEmail(Authentication authentication) {
        String to = userRepository.findByUsername(authentication.getName()).orElseThrow().getEmail();
        emailService.sendOrThrow(to, "Test Licitatii.live",
                "Salut!\n\nAcesta este un email de test trimis din pagina de administrare.\n"
                        + "Daca il primesti, notificarile pe email functioneaza.\n\n" + emailService.link("/"));
        auditService.record(authentication.getName(), AuditEntry.Action.TEST_EMAIL, null, null, null, null);
        return new Message("Emailul de test a fost trimis la " + to + ". Verifica si folderul Spam.");
    }

    public record DemoStatus(boolean exists, int items) {
    }

    // Date demo pentru prezentari (utilizatori demo_..., obiecte cu poze si oferte)
    @io.swagger.v3.oas.annotations.Operation(summary = "Cate date demo exista")
    @GetMapping("/demo-data")
    public DemoStatus demoStatus() {
        return new DemoStatus(demoDataService.exists(), 0);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Genereaza datele demo")
    @PostMapping("/demo-data")
    public DemoStatus generateDemoData(Authentication authentication) {
        int items = demoDataService.generate();
        auditService.record(authentication.getName(), AuditEntry.Action.DEMO_GENERATE, null, null, null, items + " obiecte");
        return new DemoStatus(true, items);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Sterge datele demo")
    @DeleteMapping("/demo-data")
    public DemoStatus deleteDemoData(Authentication authentication) {
        int items = demoDataService.delete();
        auditService.record(authentication.getName(), AuditEntry.Action.DEMO_DELETE, null, null, null, items + " obiecte");
        return new DemoStatus(false, items);
    }

    // Choose / reset "Licitatia saptamanii" (without a choice the busiest auction is shown)
    @io.swagger.v3.oas.annotations.Operation(summary = "Alege licitatia saptamanii")
    @PostMapping("/items/{id}/feature")
    public ResponseEntity<Void> feature(@PathVariable Integer id, Authentication authentication) {
        featuredService.choose(id);
        auditService.record(authentication.getName(), AuditEntry.Action.FEATURE_ITEM, "ITEM", id, auditService.itemLabel(id), null);
        return ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Renunta la licitatia saptamanii aleasa manual")
    @DeleteMapping("/featured")
    public ResponseEntity<Void> clearFeatured(Authentication authentication) {
        featuredService.clear();
        auditService.record(authentication.getName(), AuditEntry.Action.CLEAR_FEATURED, null, null, null, null);
        return ResponseEntity.noContent().build();
    }

    // The latest 200 admin actions
    @io.swagger.v3.oas.annotations.Operation(summary = "Jurnalul actiunilor de administrare (ultimele 200)")
    @GetMapping("/audit")
    public List<AuditEntry> audit() {
        return auditService.recent();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Statistici generale")
    @GetMapping("/stats")
    public AdminDtos.Stats stats() {
        return adminService.stats();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Date pentru grafice (oferte pe zi, vanzari pe categorii, top vanzatori)")
    @GetMapping("/charts")
    public AdminDtos.Charts charts() {
        return adminService.charts();
    }

    // Reports of suspicious items (deleting the item removes its reports too)
    @io.swagger.v3.oas.annotations.Operation(summary = "Raportarile deschise")
    @GetMapping("/reports")
    public List<AdminDtos.ReportRow> reports() {
        return adminService.openReports();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Inchide o raportare")
    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<Void> dismissReport(@PathVariable Integer id, Authentication authentication) {
        String label = auditService.reportLabel(id);
        adminService.dismissReport(id);
        auditService.record(authentication.getName(), AuditEntry.Action.DISMISS_REPORT, "REPORT", id, label, null);
        return ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Blocheaza un utilizator")
    @PostMapping("/users/{id}/block")
    public ResponseEntity<Void> block(@PathVariable Integer id, Authentication authentication) {
        adminService.setBlocked(id, true);
        auditService.record(authentication.getName(), AuditEntry.Action.BLOCK_USER, "USER", id, auditService.userLabel(id), null);
        return ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Deblocheaza un utilizator")
    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable Integer id, Authentication authentication) {
        adminService.setBlocked(id, false);
        auditService.record(authentication.getName(), AuditEntry.Action.UNBLOCK_USER, "USER", id, auditService.userLabel(id), null);
        return ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Toti utilizatorii")
    @GetMapping("/users")
    public List<AdminDtos.UserRow> users() {
        return adminService.users();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Toate obiectele")
    @GetMapping("/items")
    public List<AdminDtos.ItemRow> items() {
        return adminService.items();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Inchide acum o licitatie")
    @PostMapping("/items/{id}/close")
    public AdminDtos.ItemRow closeNow(@PathVariable Integer id, Authentication authentication) {
        AdminDtos.ItemRow row = adminService.closeNow(id);
        auditService.record(authentication.getName(), AuditEntry.Action.CLOSE_AUCTION, "ITEM", id, row.name(),
                row.winner() == null ? "fara castigator" : "castigator " + row.winner());
        return row;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Sterge un obiect (cu ofertele si pozele lui)")
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id, Authentication authentication) {
        String label = auditService.itemLabel(id);
        adminService.deleteItem(id);
        auditService.record(authentication.getName(), AuditEntry.Action.DELETE_ITEM, "ITEM", id, label, null);
        return ResponseEntity.noContent().build();
    }
}
