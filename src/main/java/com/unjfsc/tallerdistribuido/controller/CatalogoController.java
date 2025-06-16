package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.service.FavoritosService;
import com.unjfsc.tallerdistribuido.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * CLASE CONTROLADORA: Gestiona las peticiones web relacionadas con el catálogo
 * de productos. Su responsabilidad es recibir las acciones del usuario, delegar
 * la lógica de negocio a los servicios correspondientes y decidir qué vista
 * mostrar.
 */
@Controller // [CONCEPTO CLAVE]: Anotación que marca esta clase como un controlador de
			// Spring MVC.
public class CatalogoController {

	// [CONCEPTO CLAVE]: Inyección de Dependencias (Dependency Injection)
	// Spring automáticamente crea e "inyecta" una instancia de ProductoService
	// aquí.
	// Esto desacopla el controlador de la creación de sus dependencias.
	@Autowired
	private ProductoService productoService;

	@Autowired
	private FavoritosService favoritosService;

	/**
	 * MÉTODO HANDLER: Maneja las peticiones GET a la URL "/catalogo". Su único
	 * trabajo es devolver el nombre de la plantilla HTML que se debe renderizar. Ya
	 * no necesita añadir datos al 'Model' porque esa responsabilidad se ha movido a
	 * la clase 'GlobalControllerAdvice' para ser reutilizada en toda la aplicación.
	 * 
	 * @return El nombre lógico de la vista ("catalogo"), que Spring resolverá a
	 *         "catalogo.html".
	 */
	@GetMapping("/catalogo") // [CONCEPTO CLAVE]: Mapeo de Petición. Asocia este método con una URL y un
								// método HTTP (GET).
	public String verCatalogo() {
		return "catalogo"; // Devuelve el nombre de la plantilla de Thymeleaf.
	}

	/**
	 * MÉTODO HANDLER: Maneja la acción de "toggle" para añadir o quitar un producto
	 * de favoritos. Se activa cuando un usuario hace clic en el botón de corazón
	 * desde el catálogo. Utiliza el patrón Post/Redirect/Get para evitar envíos
	 * duplicados del formulario.
	 *
	 * @param productoId         El ID del producto, extraído de la URL (ej.
	 *                           /catalogo/toggle-favorito/5).
	 * @param principal          Objeto de Spring Security que contiene la
	 *                           información del usuario logueado (como su nombre).
	 * @param redirectAttributes Un objeto especial para pasar mensajes a la página
	 *                           a la que se redirige.
	 * @return Una cadena de redirección que le dice al navegador que vaya a la URL
	 *         "/catalogo".
	 */
	@PostMapping("/catalogo/toggle-favorito/{productoId}") // Asocia este método con peticiones POST a la URL dinámica.
	public String toggleFavorito(@PathVariable Long productoId, // [CONCEPTO CLAVE]: Path Variable. Extrae el valor de
																// {productoId} de la URL.
			Principal principal, // [CONCEPTO CLAVE]: Principal. Proporcionado por Spring Security con datos del
									// usuario.
			RedirectAttributes redirectAttributes) { // [CONCEPTO CLAVE]: Redirect Attributes. Para pasar datos a través
														// de una redirección.

		// [LÓGICA DE NEGOCIO]: Primera capa de validación. Si no hay usuario, no se
		// hace nada.
		if (principal == null) {
			return "redirect:/login"; // Redirige a la página de login si el usuario no está autenticado.
		}

		// [LÓGICA DE NEGOCIO]: Delegación. El controlador no sabe CÓMO se añade/quita
		// un favorito.
		// Simplemente le pide al 'FavoritosService' que lo haga.
		boolean fueAnadido = favoritosService.toggleFavorito(principal.getName(), productoId);

		// [LÓGICA DE NEGOCIO]: Feedback al usuario. Preparamos un mensaje para mostrar
		// en la siguiente página.
		if (fueAnadido) {
			// "Flash Attribute": un atributo que sobrevive a una sola redirección.
			redirectAttributes.addFlashAttribute("successMessage", "Producto añadido a favoritos.");
		} else {
			redirectAttributes.addFlashAttribute("successMessage", "Producto quitado de favoritos.");
		}

		// [CONCEPTO CLAVE]: Patrón Post-Redirect-Get (PRG).
		// Después de una operación POST exitosa, se redirige a una página GET para
		// evitar
		// que el usuario reenvíe el formulario si refresca la página.
		return "redirect:/catalogo";
	}
}