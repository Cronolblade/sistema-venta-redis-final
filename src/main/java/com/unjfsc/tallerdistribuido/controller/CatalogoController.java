package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.service.FavoritosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * CLASE CONTROLADORA: Gestiona las peticiones web para la página del catálogo.
 */
@Controller
public class CatalogoController {

	// La inyección de ProductoService ya no es necesaria aquí,
	// ya que la carga de datos se delega completamente al frontend (JavaScript).
	@Autowired
	private FavoritosService favoritosService;

	/**
	 * MÉTODO HANDLER: Maneja las peticiones GET a la URL "/catalogo". Su única
	 * responsabilidad es devolver el nombre de la plantilla HTML "esqueleto". La
	 * carga de datos la inicia el script catalogo-filter.js. Los datos globales
	 * como contadores y categorías son añadidos por GlobalControllerAdvice.
	 */
	@GetMapping("/catalogo")
	public String verCatalogo() {
		return "catalogo"; // Devuelve el nombre de la plantilla, que a su vez cargará el JS.
	}

	/**
	 * MÉTODO HANDLER: Maneja la acción de "toggle" para añadir o quitar un producto
	 * de favoritos. Utiliza el patrón Post/Redirect/Get para una mejor experiencia
	 * de usuario.
	 */
	@PostMapping("/catalogo/toggle-favorito/{productoId}")
	public String toggleFavorito(@PathVariable Long productoId, Principal principal,
			RedirectAttributes redirectAttributes) {
		if (principal == null) {
			return "redirect:/login";
		}

		// Delega la lógica de negocio al servicio.
		boolean fueAnadido = favoritosService.toggleFavorito(principal.getName(), productoId);

		// Prepara un mensaje "flash" para mostrar después de la redirección.
		if (fueAnadido) {
			redirectAttributes.addFlashAttribute("successMessage", "Producto añadido a favoritos.");
		} else {
			redirectAttributes.addFlashAttribute("successMessage", "Producto quitado de favoritos.");
		}

		// Redirige de vuelta al catálogo. El WebSocket se encargará de actualizar la
		// vista
		// si hay otros usuarios viendo la misma página.
		return "redirect:/catalogo";
	}
}