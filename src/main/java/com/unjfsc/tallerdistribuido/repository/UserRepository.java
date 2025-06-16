package com.unjfsc.tallerdistribuido.repository;

import com.unjfsc.tallerdistribuido.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// CAMBIO: Extender JpaRepository y usar Long como tipo del ID
public interface UserRepository extends JpaRepository<User, Long> {
    // Este método seguirá funcionando perfectamente con JPA
    Optional<User> findByUsername(String username);
}