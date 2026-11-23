package com.DannyU.auction_engine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username-ul este obligatoriu!")
    @Size(min = 3, max = 50, message = "Username-ul trebuie sa aiba intre 3 si 50 de caractere!")
    private String username;

    @NotBlank(message = "Email-ul este obligatoriu!")
    @Email(message = "Email-ul nu este valid!")
    @Size(max = 100, message = "Email-ul poate avea maxim 100 de caractere!")
    private String email;

    @NotBlank(message = "Parola este obligatorie!")
    @Size(min = 6, message = "Parola trebuie sa aiba minim 6 caractere!")
    private String password;
}
