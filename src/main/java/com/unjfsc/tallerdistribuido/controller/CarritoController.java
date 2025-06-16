package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.dto.UpdateNotification;
import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.service.CarritoService;
import com.unjfsc.tallerdistribuido.service.InsufficientStockException;
import com.unjfsc.tallerdistribuido.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

	@Autowired
	private CarritoService carritoService;

	@Autowired
	private ProductoService productoService;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private CacheManager cacheManager;

	@GetMapping
	public String verCarrito(Model model, Principal principal) {
		if (principal == null)
			return "redirect:/login";

		Map<Producto, Integer> productosEnCarrito = carritoService.getProductosEnCarrito(principal.getName());
		model.addAttribute("productosEnCarrito", productosEnCarrito);

		double total = productosEnCarrito.entrySet().stream()
				.mapToDouble(entry -> entry.getKey().getPrecio() * entry.getValue()).sum();
		model.addAttribute("totalCarrito", total);

		return "carrito";
	}

	@PostMapping("/agregar/{productoId}")
	public String agregarAlCarrito(@PathVariable Long productoId, @RequestParam("cantidad") int cantidad,
			Principal principal, RedirectAttributes redirectAttributes) {

		// --- LOG DE DEPURACIÓN ---
		// Este mensaje nos confirmará si la petición está llegando a este método.
		System.out.println(
				"--- DENTRO DE CarritoController.agregarAlCarrito --- ID: " + productoId + ", Cantidad: " + cantidad);

		if (principal == null)
			return "redirect:/login";

		try {
			carritoService.agregarProducto(principal.getName(), productoId, cantidad);
			redirectAttributes.addFlashAttribute("successMessage", "Producto añadido al carrito!");
		} catch (InsufficientStockException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Error al añadir el producto.");
			e.printStackTrace(); // Imprime el error completo en la consola de Spring para más detalles.
		}

		// Redirigimos al catálogo para que el usuario pueda seguir comprando.
		// La actualización de los contadores se manejará en la siguiente carga de
		// página.
		return "redirect:/catalogo";
	}

	@PostMapping("/eliminar/{productoId}")
	public String eliminarDelCarrito(@PathVariable Long productoId, Principal principal) {
		if (principal == null)
			return "redirect:/login";
		carritoService.eliminarProducto(principal.getName(), productoId);
		return "redirect:/carrito";
	}

	@PostMapping("/comprar")
	public String realizarCompra(Principal principal, RedirectAttributes redirectAttributes) {
		if (principal == null)
			return "redirect:/login";

		try {
			// 1. Ejecuta la lógica de negocio y la transacción de la base de datos.
			carritoService.realizarCompra(principal.getName());

			// 2. Después de que la transacción ha terminado con éxito, notificamos a todos
			// los clientes.
			productoService.notifyClientsOfUpdate();

			redirectAttributes.addFlashAttribute("successMessage", "¡Compra realizada con éxito!");
			return "redirect:/catalogo";

		} catch (InsufficientStockException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/carrito";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Ocurrió un error al procesar la compra.");
			e.printStackTrace();
			return "redirect:/carrito";
		}
	}
}