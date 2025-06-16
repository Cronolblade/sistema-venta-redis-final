package com.unjfsc.tallerdistribuido.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

	/**
	 * Este método maneja las peticiones a la URL "/ventas" y devuelve el nombre de
	 * la plantilla de Thymeleaf que se debe mostrar.
	 */
	@GetMapping("/ventas")
	public String showVentasPage() {
		return "ventas"; // Esto le dice a Spring que renderice el archivo ventas.html
	}
}