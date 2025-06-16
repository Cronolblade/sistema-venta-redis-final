package com.unjfsc.tallerdistribuido.config;

import com.unjfsc.tallerdistribuido.service.CarritoService;
import com.unjfsc.tallerdistribuido.service.FavoritosService;
import com.unjfsc.tallerdistribuido.service.ProductoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Set;

@ControllerAdvice
public class GlobalControllerAdvice {

	@Autowired
	private CarritoService carritoService;

	@Autowired
	private ProductoService productoService;

	@Autowired
	private FavoritosService favoritosService;

	@Autowired
	private ObjectMapper objectMapper; // Para convertir el Set a un string JSON

	@ModelAttribute
	public void addGlobalAttributes(Model model, Principal principal) {
		if (principal != null) {
			// Contadores para carrito y favoritos
			model.addAttribute("cartItemCount", carritoService.getCartItemCount(principal.getName()));
			model.addAttribute("favoritosCount", favoritosService.getFavoritosCount(principal.getName()));

			// --- CAMBIO IMPORTANTE ---
			// Obtenemos los IDs de favoritos y los pasamos al modelo.
			Set<String> favoritosIds = favoritosService.getProductosFavoritosIds(principal.getName());
			try {
				// Convertimos el Set de IDs a un string JSON para que JS pueda leerlo
				// fácilmente.
				model.addAttribute("favoritosIdsJson", objectMapper.writeValueAsString(favoritosIds));
			} catch (JsonProcessingException e) {
				// En caso de error, pasamos un array JSON vacío.
				model.addAttribute("favoritosIdsJson", "[]");
			}

		} else {
			// Valores por defecto para usuarios no logueados
			model.addAttribute("cartItemCount", 0L);
			model.addAttribute("favoritosCount", 0L);
			model.addAttribute("favoritosIdsJson", "[]");
		}

		// Se mantiene la lógica para las categorías
		Set<String> categorias = productoService.findDistinctCategorias();
		model.addAttribute("categoriasUnicas", categorias);
	}
}