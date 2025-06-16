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

	// Se mantiene igual
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<Producto> getAllProductos() {
		return productoService.findAll();
	}

	// --- NUEVO ENDPOINT PARA BÚSQUEDA PÚBLICA ---
	// Este endpoint es usado por el JavaScript del catálogo y no necesita rol de
	// ADMIN
	@GetMapping("/search")
	public List<Producto> searchProductos(@RequestParam(required = false, defaultValue = "") String name,
			@RequestParam(required = false, defaultValue = "") String category) {
		return productoService.search(name, category);
	}

	// El resto de los métodos (POST, PUT, DELETE) se mantienen iguales...
	// ...
}