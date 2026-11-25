package com.DannyU.auction_engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username-ul este obligatoriu!")
    @io.swagger.v3.oas.annotations.media.Schema(example = "ana_pop")
    private String username;

    @NotBlank(message = "Parola este obligatorie!")
    @io.swagger.v3.oas.annotations.media.Schema(example = "parola123")
    private String password;
}
