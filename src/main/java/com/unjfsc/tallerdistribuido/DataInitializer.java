package com.unjfsc.tallerdistribuido;

import com.unjfsc.tallerdistribuido.model.User;
import com.unjfsc.tallerdistribuido.repository.UserRepository; // El repo JPA
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    // No hay cambios en las dependencias ni en el constructor
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${admin.username}") private String adminUsername;
    @Value("${admin.password}") private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // La lógica interna sigue funcionando porque se basa en la interfaz del repositorio
        createAdminUser(); 
    }

    private void createAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRoles(Set.of("ROLE_ADMIN", "ROLE_USER")); // Un admin también puede ser usuario
            
            userRepository.save(admin);
            System.out.println(">>> Usuario administrador '" + adminUsername + "' creado en MySQL.");
        } else {
            System.out.println(">>> El usuario administrador '" + adminUsername + "' ya existe en MySQL.");
        }
    }
}