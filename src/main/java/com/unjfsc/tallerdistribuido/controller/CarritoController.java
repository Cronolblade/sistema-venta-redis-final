package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.service.CarritoService;
import com.unjfsc.tallerdistribuido.service.InsufficientStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

/**
 * CLASE CONTROLADORA: Gestiona todas las interacciones del usuario con su
 * carrito de compras. Define los endpoints para ver, añadir, eliminar y comprar
 * los productos del carrito.
 */
@Controller
@RequestMapping("/carrito") // [CONCEPTO CLAVE]: Todas las rutas de este controlador parten de "/carrito".
public class CarritoController {

	@Autowired
	private CarritoService carritoService; // Inyección del servicio que contiene la lógica de negocio.

	/**
	 * MÉTODO HANDLER (Leer): Muestra la página del carrito de compras. Obtiene los
	 * productos del carrito y calcula el total para mostrarlos en la vista.
	 */
	@GetMapping // Mapea a GET /carrito
	public String verCarrito(Model model, Principal principal) {
		if (principal == null)
			return "redirect:/login";

		// [LÓGICA DE NEGOCIO]: Pide al servicio el contenido del carrito.
		// El servicio devuelve un Mapa donde la clave es el objeto Producto y el valor
		// es la cantidad.
		Map<Producto, Integer> productosEnCarrito = carritoService.getProductosEnCarrito(principal.getName());
		model.addAttribute("productosEnCarrito", productosEnCarrito);

		// [LÓGICA DE PRESENTACIÓN]: Calcula el costo total del carrito.
		// Se usa un stream para iterar sobre el mapa, multiplicar precio por cantidad y
		// sumar todo.
		double total = productosEnCarrito.entrySet().stream()
				.mapToDouble(entry -> entry.getKey().getPrecio() * entry.getValue()).sum();
		model.addAttribute("totalCarrito", total);

		return "carrito"; // Devuelve el nombre de la plantilla "carrito.html".
	}

	/**
	 * MÉTODO HANDLER (Crear/Actualizar): Añade un producto con una cantidad
	 * específica al carrito. Se llama desde los formularios en las páginas de
	 * catálogo y favoritos.
	 * 
	 * @param productoId El ID del producto a añadir.
	 * @param cantidad   La cantidad del producto, obtenida del formulario.
	 */
	@PostMapping("/agregar/{productoId}") // Mapea a POST /carrito/agregar/5
	public String agregarAlCarrito(@PathVariable Long productoId, @RequestParam("cantidad") int cantidad, // [CONCEPTO
																											// CLAVE]:
																											// RequestParam.
																											// Extrae un
																											// parámetro
																											// del
																											// formulario
																											// (en este
																											// caso, del
																											// input con
																											// name="cantidad").
			Principal principal, RedirectAttributes redirectAttributes) {

		if (principal == null)
			return "redirect:/login";

		// [CONCEPTO CLAVE]: Manejo de Excepciones de Negocio.
		// Se usa un bloque try-catch para manejar errores esperados, como la falta de
		// stock.
		try {
			// Delega la lógica al servicio.
			carritoService.agregarProducto(principal.getName(), productoId, cantidad);
			redirectAttributes.addFlashAttribute("successMessage", "Producto añadido al carrito!");
		} catch (InsufficientStockException e) {
			// Si el servicio lanza esta excepción específica, mostramos un mensaje de error
			// amigable.
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		} catch (Exception e) {
			// Captura cualquier otro error inesperado.
			redirectAttributes.addFlashAttribute("errorMessage", "Error al añadir el producto.");
		}

		// Redirige al catálogo después de la acción.
		return "redirect:/catalogo";
	}

	/**
	 * MÉTODO HANDLER (Borrar): Elimina un producto completo del carrito.
	 */
	@PostMapping("/eliminar/{productoId}") // Mapea a POST /carrito/eliminar/5
	public String eliminarDelCarrito(@PathVariable Long productoId, Principal principal) {
		if (principal == null)
			return "redirect:/login";

		carritoService.eliminarProducto(principal.getName(), productoId);

		// Redirige a la misma página del carrito para ver el resultado.
		return "redirect:/carrito";
	}

	/**
	 * MÉTODO HANDLER (Acción Final): Procesa la compra final. Descuenta el stock de
	 * los productos y vacía el carrito.
	 */
	@PostMapping("/comprar") // Mapea a POST /carrito/comprar
	public String realizarCompra(Principal principal, RedirectAttributes redirectAttributes) {
		if (principal == null)
			return "redirect:/login";

		// De nuevo, se usa try-catch para manejar posibles errores de negocio durante
		// la compra.
		try {
			carritoService.realizarCompra(principal.getName());
			redirectAttributes.addFlashAttribute("successMessage", "¡Compra realizada con éxito!");
			return "redirect:/catalogo"; // Devuelve al usuario al catálogo después de una compra exitosa.
		} catch (InsufficientStockException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/carrito"; // Si no hay stock, lo devuelve al carrito para que ajuste su pedido.
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Ocurrió un error al procesar la compra.");
			return "redirect:/carrito";
		}
	}
}