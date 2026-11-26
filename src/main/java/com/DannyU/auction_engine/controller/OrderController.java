package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Orders with escrow after a sale (see OrderService). Buyer and seller act on their own orders; admins settle disputes.
@Tag(name = "Comenzi", description = "Dupa vanzare: plata in escrow, expedierea cu AWB, confirmarea primirii si disputele.")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    public record ShipRequest(String courier, String awb) {
    }

    public record DisputeRequest(String reason) {
    }

    public record ResolveRequest(boolean refund, String note) {
    }

    @Operation(summary = "Comenzile mele (cumparaturi si vanzari)")
    @GetMapping("/api/orders/mine")
    public OrderService.MyOrders mine(Authentication authentication) {
        return orderService.mine(authentication.getName());
    }

    @Operation(summary = "Curierii acceptati la expediere")
    @GetMapping("/api/orders/couriers")
    public List<String> couriers() {
        return OrderService.COURIERS;
    }

    @Operation(summary = "Comanda unui obiect vandut (doar cumparatorul, vanzatorul sau un admin); 204 daca nu exista")
    @GetMapping("/api/items/{id}/order")
    public ResponseEntity<OrderService.OrderView> forItem(@PathVariable Integer id, Authentication authentication) {
        return orderService.forItem(id, authentication.getName()).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Vanzatorul marcheaza expedierea (curier + AWB sau predare personala)")
    @PostMapping("/api/orders/{id}/ship")
    public OrderService.OrderView ship(@PathVariable Integer id, @RequestBody ShipRequest request, Authentication authentication) {
        return orderService.ship(id, authentication.getName(), request.courier(), request.awb());
    }

    @Operation(summary = "Cumparatorul confirma primirea; banii ajung la vanzator")
    @PostMapping("/api/orders/{id}/confirm")
    public OrderService.OrderView confirm(@PathVariable Integer id, Authentication authentication) {
        return orderService.confirm(id, authentication.getName());
    }

    @Operation(summary = "Cumparatorul deschide o disputa; banii raman blocati")
    @PostMapping("/api/orders/{id}/dispute")
    public OrderService.OrderView dispute(@PathVariable Integer id, @RequestBody DisputeRequest request, Authentication authentication) {
        return orderService.dispute(id, authentication.getName(), request.reason());
    }

    @Operation(summary = "Disputele deschise")
    @GetMapping("/api/admin/disputes")
    public List<OrderService.OrderView> disputes(Authentication authentication) {
        return orderService.disputes(authentication.getName());
    }

    @Operation(summary = "Rezolva o disputa: rambursare catre cumparator sau plata catre vanzator")
    @PostMapping("/api/admin/orders/{id}/resolve")
    public OrderService.OrderView resolve(@PathVariable Integer id, @RequestBody ResolveRequest request, Authentication authentication) {
        return orderService.resolve(id, authentication.getName(), request.refund(), request.note());
    }
}
