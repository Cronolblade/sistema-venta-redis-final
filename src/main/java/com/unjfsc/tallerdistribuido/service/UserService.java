package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.User;
import com.unjfsc.tallerdistribuido.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * CLASE DE SERVICIO: Gestiona la lógica de negocio para la entidad User. Se
 * encarga del registro de nuevos usuarios y de la búsqueda de usuarios
 * existentes.
 */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Busca un usuario por su nombre de usuario. El resultado se cachea.
	 * 
	 * @Cacheable: Cuando se busca un usuario (ej. durante el login), si ya está en
	 *             la caché "users", se devuelve desde Redis. Si no, se consulta
	 *             MySQL y se guarda en la caché. La clave en Redis será el nombre
	 *             de usuario.
	 */
	@Cacheable(value = "users", key = "#username")
	public Optional<User> findByUsername(String username) {
		System.out.println("--- BUSCANDO USUARIO EN MYSQL (CACHE MISS) USER: " + username + " ---");
		return userRepository.findByUsername(username);
	}

	/**
	 * Registra un nuevo usuario en el sistema.
	 * 
	 * @CacheEvict: Después de guardar el nuevo usuario en MySQL, esta anotación se
	 *              asegura de borrar cualquier entrada de caché que pudiera existir
	 *              con ese nombre de usuario. Esto es importante por si había una
	 *              entrada de caché "negativa" (que indicaba que el usuario no
	 *              existía).
	 */
	@CacheEvict(value = "users", key = "#user.username")
	public void registerNewUser(User user) {
		// 1. Verificar si el usuario ya existe en la base de datos.
		if (userRepository.findByUsername(user.getUsername()).isPresent()) {
			throw new IllegalStateException("El nombre de usuario '" + user.getUsername() + "' ya está en uso.");
		}
		System.out.println("--- REGISTRANDO NUEVO USUARIO EN MYSQL Y BORRANDO CACHÉ: " + user.getUsername() + " ---");

		// 2. Crear y configurar el nuevo objeto User.
		User newUser = new User();
		newUser.setUsername(user.getUsername());
		// Se cifra la contraseña antes de guardarla. NUNCA se guarda en texto plano.
		newUser.setPassword(passwordEncoder.encode(user.getPassword()));
		newUser.setRoles(Set.of("ROLE_USER"));

		// 3. Guardar el nuevo usuario en MySQL.
		userRepository.save(newUser);
	}
}