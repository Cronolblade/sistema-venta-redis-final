package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.service.FavoritosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/favoritos")
public class FavoritosController {

	@Autowired
	private FavoritosService favoritosService;

	@GetMapping
	public String verFavoritos(Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		// Esta llamada ahora es muy eficiente gracias a la optimización en el servicio.
		model.addAttribute("productosFavoritos", favoritosService.getProductosFavoritos(principal.getName()));
		return "favoritos";
	}

	@PostMapping("/agregar/{productoId}")
	public String agregarAFavoritos(@PathVariable Long productoId, Principal principal,
			RedirectAttributes redirectAttributes) {
		if (principal == null) {
			return "redirect:/login";
		}
		favoritosService.agregarFavorito(principal.getName(), productoId);
		redirectAttributes.addFlashAttribute("successMessage", "Producto añadido a favoritos!");
		return "redirect:/catalogo";
	}

	@PostMapping("/eliminar/{productoId}")
	public String eliminarDeFavoritos(@PathVariable Long productoId, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		favoritosService.eliminarFavorito(principal.getName(), productoId);
		return "redirect:/favoritos";
	}
}