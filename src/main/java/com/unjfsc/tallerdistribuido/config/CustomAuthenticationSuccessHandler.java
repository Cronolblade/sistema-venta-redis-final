package com.unjfsc.tallerdistribuido.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * CLASE DE MANEJO DE EVENTOS: Define una lógica personalizada para ejecutar
 * después de una autenticación exitosa. Su principal propósito aquí es
 * redirigir a los usuarios a diferentes páginas de inicio según su rol (ADMIN o
 * USER).
 */
@Component // [CONCEPTO CLAVE]: Componente de Spring. Marca esta clase para que Spring la
			// descubra y la gestione como un bean.
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	/**
	 * MÉTODO DE CALLBACK: Este método es invocado automáticamente por Spring
	 * Security cuando un usuario ha proporcionado credenciales válidas y ha sido
	 * autenticado.
	 *
	 * @param request        La petición HTTP original.
	 * @param response       La respuesta HTTP que se enviará al navegador.
	 * @param authentication El objeto 'Authentication' que contiene todos los
	 *                       detalles del usuario autenticado, incluyendo su nombre
	 *                       de usuario, roles (authorities), etc.
	 */
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		// [LÓGICA DE NEGOCIO]: Se establece una URL de redirección por defecto.
		// Si por alguna razón el usuario no tiene un rol reconocido, se le enviará
		// aquí.
		String redirectUrl = "/login?error";

		// [CONCEPTO CLAVE]: Obtención de Roles (Authorities).
		// El objeto 'authentication' contiene la colección de roles asignados al
		// usuario.
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

		// [LÓGICA DE NEGOCIO]: Se itera sobre los roles del usuario.
		for (GrantedAuthority grantedAuthority : authorities) {
			// Se comprueba si el usuario tiene el rol "ROLE_ADMIN".
			if (grantedAuthority.getAuthority().equals("ROLE_ADMIN")) {
				redirectUrl = "/ventas"; // Si es ADMIN, se le redirige al panel de ventas.
				break; // Se detiene el bucle porque ya hemos encontrado el rol más importante.
			}
			// Si no es ADMIN, se comprueba si tiene el rol "ROLE_USER".
			else if (grantedAuthority.getAuthority().equals("ROLE_USER")) {
				redirectUrl = "/catalogo"; // Si es USER, se le redirige al catálogo.
				break; // Se detiene el bucle.
			}
		}

		// [ACCIÓN FINAL]: Se envía una redirección HTTP al navegador del usuario.
		// El navegador recibirá esta respuesta y automáticamente hará una nueva
		// petición a la 'redirectUrl'.
		response.sendRedirect(redirectUrl);
	}
}