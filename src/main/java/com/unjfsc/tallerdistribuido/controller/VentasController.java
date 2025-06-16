package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class VentasController {

	@Autowired
	private ProductoService productoService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<Producto> getAllProductos() {
		// Para la carga inicial del panel de admin, usamos el método search
		// para ser consistentes con el frontend del cliente.
		return productoService.search("", "");
	}

	@GetMapping("/search")
	public List<Producto> searchProductos(@RequestParam(required = false, defaultValue = "") String name,
			@RequestParam(required = false, defaultValue = "") String category) {
		return productoService.search(name, category);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Producto> createProducto(@RequestBody Producto producto) {
		// Paso 1: Guardar el producto (esto invalida la caché).
		Producto savedProducto = productoService.save(producto);
		// Paso 2: Notificar a todos los clientes del cambio.
		productoService.notifyClientsOfUpdate();
		return new ResponseEntity<>(savedProducto, HttpStatus.CREATED);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Producto> updateProducto(@PathVariable Long id, @RequestBody Producto productoDetails) {
		if (productoService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		productoDetails.setId(id);

		// Paso 1: Guardar el producto (esto invalida la caché).
		Producto updatedProducto = productoService.save(productoDetails);
		// Paso 2: Notificar a todos los clientes del cambio.
		productoService.notifyClientsOfUpdate();

		return ResponseEntity.ok(updatedProducto);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
		if (productoService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		// Paso 1: Borrar el producto (esto invalida la caché).
		productoService.deleteById(id);
		// Paso 2: Notificar a todos los clientes del cambio.
		productoService.notifyClientsOfUpdate();

		return ResponseEntity.noContent().build();
	}
}