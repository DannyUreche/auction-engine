package com.DannyU.auction_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// Decide cine este administrator: username-urile din setarea app.admin-usernames
// (variabila de mediu ADMIN_USERNAMES pe server, ex: "danny,maria")
@Component
public class AdminAccess {

    private final Set<String> adminUsernames;

    public AdminAccess(@Value("${app.admin-usernames:}") String adminUsernames) {
        this.adminUsernames = Arrays.stream(adminUsernames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(String username) {
        return username != null && adminUsernames.contains(username);
    }
}
