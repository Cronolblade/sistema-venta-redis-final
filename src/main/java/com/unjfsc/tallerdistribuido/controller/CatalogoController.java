package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.service.FavoritosService;
import com.unjfsc.tallerdistribuido.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class CatalogoController {

	@Autowired
	private ProductoService productoService;

	@Autowired
	private FavoritosService favoritosService;

	// Este método se encarga de mostrar la página del catálogo.
	// No necesita cambios, ya que los atributos globales (como 'favoritosIds')
	// se inyectan a través del GlobalControllerAdvice.
	@GetMapping("/catalogo")
	public String verCatalogo() {
		return "catalogo";
	}

	/**
	 * Endpoint que maneja la acción de añadir/quitar un producto de favoritos desde
	 * la página del catálogo.
	 */
	@PostMapping("/catalogo/toggle-favorito/{productoId}")
	public String toggleFavorito(@PathVariable Long productoId, Principal principal,
			RedirectAttributes redirectAttributes) {
		if (principal == null) {
			return "redirect:/login";
		}

		// Llama al nuevo método de servicio que hace el "toggle"
		boolean fueAnadido = favoritosService.toggleFavorito(principal.getName(), productoId);

		// Añade un mensaje flash para notificar al usuario de la acción realizada.
		if (fueAnadido) {
			redirectAttributes.addFlashAttribute("successMessage", "Producto añadido a favoritos.");
		} else {
			redirectAttributes.addFlashAttribute("successMessage", "Producto quitado de favoritos.");
		}

		// Redirige de vuelta a la página del catálogo para que se vea el cambio.
		return "redirect:/catalogo";
	}
}