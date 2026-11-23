package com.DannyU.auction_engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username-ul este obligatoriu!")
    private String username;

    @NotBlank(message = "Parola este obligatorie!")
    private String password;
}
