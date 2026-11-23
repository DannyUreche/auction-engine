package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminAccess adminAccess;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizatorul nu a fost gasit: " + username));

        // Administratorii primesc rolul ADMIN (necesar pentru rutele /api/admin/**)
        List<SimpleGrantedAuthority> roles = adminAccess.isAdmin(user.getUsername())
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of();

        // Returnam un obiect UserDetails pe care il intelege Spring Security
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), roles);
    }
}