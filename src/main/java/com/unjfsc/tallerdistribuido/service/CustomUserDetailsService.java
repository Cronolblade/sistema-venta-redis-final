package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * CLASE DE SERVICIO DE SEGURIDAD: Implementa la interfaz UserDetailsService de
 * Spring Security. Su ÚNICA responsabilidad es tomar un 'username' (String) y
 * devolver un objeto 'UserDetails' que Spring Security pueda entender. Es el
 * adaptador entre tu entidad 'User' y el contrato 'UserDetails' de Spring
 * Security.
 */
@Service // [CONCEPTO CLAVE]: Marca esta clase como un servicio, haciéndola un bean
			// gestionado por Spring.
public class CustomUserDetailsService implements UserDetailsService {

	// [CONCEPTO CLAVE]: Delegación a otro servicio.
	// En lugar de inyectar el Repositorio directamente, inyecta el UserService.
	// Esto es una buena práctica porque la lógica de cómo encontrar un usuario (y
	// su caché)
	// pertenece al UserService. Este servicio solo se encarga de la "traducción"
	// para Spring Security.
	private final UserService userService;

	public CustomUserDetailsService(UserService userService) {
		this.userService = userService;
	}

	/**
	 * MÉTODO DE CONTRATO (Override): Este es el único método definido por la
	 * interfaz UserDetailsService. Spring Security lo llamará automáticamente
	 * durante el proceso de autenticación, pasándole el nombre de usuario que el
	 * usuario introdujo en el formulario de login.
	 *
	 * @param username El nombre de usuario a buscar.
	 * @return Un objeto UserDetails que contiene la información del usuario.
	 * @throws UsernameNotFoundException Si no se encuentra un usuario con ese
	 *                                   nombre de usuario.
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// [LÓGICA DE NEGOCIO]: Delegación. Llama al UserService para encontrar al
		// usuario.
		// Gracias a la anotación @Cacheable en `userService.findByUsername()`, esta
		// operación
		// primero buscará en la caché de Redis antes de ir a la base de datos MySQL.
		User user = userService.findByUsername(username)
				// [CONCEPTO CLAVE]: Manejo de "No Encontrado".
				// Si el Optional devuelto por findByUsername está vacío, se lanza la excepción
				// que Spring Security espera para un usuario no válido.
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

		// [CONCEPTO CLAVE]: Adaptación de Modelo.
		// Aquí "traducimos" nuestro objeto `com.unjfsc.tallerdistribuido.model.User`
		// a un objeto `org.springframework.security.core.userdetails.User`, que es la
		// implementación estándar de UserDetails que Spring proporciona.
		return new org.springframework.security.core.userdetails.User(user.getUsername(), // 1er parámetro: el nombre de
																							// usuario.
				user.getPassword(), // 2do parámetro: la contraseña HASHEADA desde la base de datos.

				// 3er parámetro: la colección de roles (authorities).
				// Spring Security necesita que los roles sean objetos `GrantedAuthority`.
				// Usamos un stream para transformar cada String de rol (ej. "ROLE_ADMIN")
				// en un objeto `SimpleGrantedAuthority`.
				user.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
	}
}