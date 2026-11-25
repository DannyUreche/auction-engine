package com.DannyU.auction_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    // Missing when a second step is needed
    private String token;
    // Two-factor login: the password was right, now send the code with this ticket to /api/auth/login/2fa
    private boolean twoFactorRequired;
    private String twoFactorTicket;

    public AuthResponse(String token) {
        this(token, false, null);
    }
}