package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.User;
import com.unjfsc.tallerdistribuido.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Busca un usuario por su nombre de usuario. El resultado se cachea para
	 * acelerar futuras búsquedas del mismo usuario.
	 */
	@Cacheable(value = "users", key = "#username")
	public Optional<User> findByUsername(String username) {
		System.out.println("--- BUSCANDO USUARIO EN MYSQL (CACHE MISS) USER: " + username + " ---");
		return userRepository.findByUsername(username);
	}

	/**
	 * Registra un nuevo usuario. Tras guardarlo, invalida cualquier entrada de
	 * caché que pudiera existir para ese usuario, asegurando que la próxima
	 * búsqueda obtenga los datos frescos.
	 */
	@CacheEvict(value = "users", key = "#user.username")
	public void registerNewUser(User user) {
		// 1. Verificar si el usuario ya existe.
		// La llamada a findByUsername aquí NO usará la caché, porque se está llamando
		// desde dentro de la misma clase. Spring AOP necesita un proxy para interceptar
		// la llamada.
		// Para este caso de uso, no es un problema.
		if (userRepository.findByUsername(user.getUsername()).isPresent()) {
			throw new IllegalStateException("El nombre de usuario '" + user.getUsername() + "' ya está en uso.");
		}
		System.out.println("--- REGISTRANDO NUEVO USUARIO EN MYSQL Y BORRANDO CACHÉ: " + user.getUsername() + " ---");

		// 2. Crear el nuevo usuario
		User newUser = new User();
		newUser.setUsername(user.getUsername());
		newUser.setPassword(passwordEncoder.encode(user.getPassword()));
		newUser.setRoles(Set.of("ROLE_USER"));

		// 3. Guardar el usuario en la base de datos MySQL
		userRepository.save(newUser);
	}
}