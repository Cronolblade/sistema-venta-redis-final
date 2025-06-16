package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.model.User;
import com.unjfsc.tallerdistribuido.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

	private final UserService userService;

	public HomeController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/")
	public String home() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated()
				&& !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
			if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
				return "redirect:/ventas";
			}
			if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
				return "redirect:/catalogo";
			}
		}
		return "redirect:/login";
	}

	@GetMapping("/registro")
	public String showRegistrationForm(Model model) {
		model.addAttribute("user", new User());
		return "registro";
	}

	@PostMapping("/registro")
	public String registerUser(@ModelAttribute("user") User user, Model model) {
		try {
			userService.registerNewUser(user);
			return "redirect:/login?registroExitoso";
		} catch (IllegalStateException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return "registro";
		}
	}
}