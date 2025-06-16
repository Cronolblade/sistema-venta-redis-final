package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService; // <-- CAMBIO: Inyectamos UserService

    public CustomUserDetailsService(UserService userService) { // <-- CAMBIO: Constructor actualizado
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // La llamada al servicio usará el método cacheable
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(), 
            user.getPassword(),
            user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList())
        );
    }
}