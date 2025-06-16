package com.unjfsc.tallerdistribuido.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

	// Ya no necesita el UserService, su única función es mostrar la página de
	// login.

	@GetMapping("/login")
	public String login() {
		return "login";
	}

}