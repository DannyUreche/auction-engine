package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.dto.AdminDtos;
import com.DannyU.auction_engine.service.AdminService;
import com.DannyU.auction_engine.service.DemoDataService;
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

    @GetMapping("/stats")
    public AdminDtos.Stats stats() {
        return adminService.stats();
    }

    @GetMapping("/charts")
    public AdminDtos.Charts charts() {
        return adminService.charts();
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
