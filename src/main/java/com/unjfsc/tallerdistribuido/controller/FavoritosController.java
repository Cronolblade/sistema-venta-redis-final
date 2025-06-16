package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.service.FavoritosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * CLASE CONTROLADORA: Gestiona las peticiones web para la página de "Mis
 * Favoritos". Su responsabilidad es mostrar la lista de productos favoritos y
 * manejar la eliminación de productos de esa lista.
 */
@Controller
@RequestMapping("/favoritos") // [CONCEPTO CLAVE]: Mapeo a Nivel de Clase. Todas las rutas de este controlador
								// empezarán con "/favoritos".
public class FavoritosController {

	// [CONCEPTO CLAVE]: Inyección de Dependencias.
	// El controlador necesita el FavoritosService para realizar las operaciones de
	// negocio.
	@Autowired
	private FavoritosService favoritosService;

	/**
	 * MÉTODO HANDLER (Leer): Maneja las peticiones GET a "/favoritos". Su objetivo
	 * es obtener la lista de productos favoritos del usuario y mostrarla.
	 *
	 * @param model     El "maletín" de datos para pasar información a la vista
	 *                  (plantilla HTML).
	 * @param principal Objeto de Spring Security con la información del usuario
	 *                  logueado.
	 * @return El nombre de la vista "favoritos" para ser renderizada.
	 */
	@GetMapping // Mapea a GET "/favoritos" porque hereda el @RequestMapping de la clase.
	public String verFavoritos(Model model, Principal principal) {
		// [LÓGICA DE NEGOCIO]: Validación de seguridad.
		if (principal == null) {
			return "redirect:/login";
		}

		// [LÓGICA DE NEGOCIO]: Delegación.
		// 1. Llama al servicio para obtener la lista de objetos Producto completos que
		// son favoritos.
		// 2. El servicio se encarga de la lógica compleja: primero consulta Redis para
		// los IDs, luego MySQL para los detalles.
		// 3. El controlador no necesita saber nada de esa complejidad.
		model.addAttribute("productosFavoritos", favoritosService.getProductosFavoritos(principal.getName()));

		// Devuelve el nombre de la plantilla HTML "favoritos.html".
		return "favoritos";
	}

	/**
	 * MÉTODO HANDLER (Crear/Añadir): Maneja las peticiones POST a
	 * "/favoritos/agregar/{productoId}". Este método es llamado desde la página de
	 * favoritos si decides añadirlo al carrito desde allí. NOTA: Este método está
	 * obsoleto ya que la lógica de "toggle" en CatalogoController es más moderna.
	 * Podría eliminarse si no se usa, o mantenerse por si se necesita desde otro
	 * lugar.
	 */
	@PostMapping("/agregar/{productoId}")
	public String agregarAFavoritos(@PathVariable Long productoId, Principal principal,
			RedirectAttributes redirectAttributes) {
		if (principal == null) {
			return "redirect:/login";
		}
		// [LÓGICA DE NEGOCIO]: Delegación. Llama al método simple 'agregarFavorito'.
		favoritosService.agregarFavorito(principal.getName(), productoId);
		redirectAttributes.addFlashAttribute("successMessage", "Producto añadido a favoritos!");

		// Redirige al catálogo para que el usuario pueda seguir comprando.
		return "redirect:/catalogo";
	}

	/**
	 * MÉTODO HANDLER (Borrar): Maneja las peticiones POST a
	 * "/favoritos/eliminar/{productoId}". Se activa cuando el usuario hace clic en
	 * el botón "Quitar" en la página de "Mis Favoritos".
	 *
	 * @param productoId El ID del producto a eliminar de la lista de favoritos.
	 * @param principal  Información del usuario actual.
	 * @return Una redirección a la misma página de favoritos para ver la lista
	 *         actualizada.
	 */
	@PostMapping("/eliminar/{productoId}") // Mapea a POST "/favoritos/eliminar/5", por ejemplo.
	public String eliminarDeFavoritos(@PathVariable Long productoId, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}

		// [LÓGICA DE NEGOCIO]: Delegación. Llama al servicio para que elimine el ID del
		// set de Redis.
		favoritosService.eliminarFavorito(principal.getName(), productoId);

		// Redirige de vuelta a la página de favoritos para que el usuario vea el
		// producto desaparecer.
		return "redirect:/favoritos";
	}
}