package com.unjfsc.tallerdistribuido.controller;

import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.service.CarritoService;
import com.unjfsc.tallerdistribuido.service.InsufficientStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired
    private CarritoService carritoService;

    @GetMapping
    public String verCarrito(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        
        Map<Producto, Integer> productosEnCarrito = carritoService.getProductosEnCarrito(principal.getName());
        model.addAttribute("productosEnCarrito", productosEnCarrito);

        double total = productosEnCarrito.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrecio() * entry.getValue())
                .sum();
        model.addAttribute("totalCarrito", total);

        return "carrito";
    }

    @PostMapping("/agregar/{productoId}")
    public String agregarAlCarrito(@PathVariable Long productoId, @RequestParam("cantidad") int cantidad, // <-- CAMBIO: ID es Long
                                   Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        try {
            carritoService.agregarProducto(principal.getName(), productoId, cantidad);
            redirectAttributes.addFlashAttribute("successMessage", "Producto añadido al carrito!");
        } catch (InsufficientStockException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al añadir el producto: " + e.getMessage());
        }
        return "redirect:/catalogo";
    }

    @PostMapping("/eliminar/{productoId}")
    public String eliminarDelCarrito(@PathVariable Long productoId, Principal principal) { // <-- CAMBIO: ID es Long
        if (principal == null) return "redirect:/login";
        carritoService.eliminarProducto(principal.getName(), productoId);
        return "redirect:/carrito";
    }

    @PostMapping("/comprar")
    public String realizarCompra(Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        try {
            carritoService.realizarCompra(principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "¡Compra realizada con éxito!");
            return "redirect:/catalogo";
        } catch (InsufficientStockException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/carrito";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ocurrió un error al procesar la compra.");
            return "redirect:/carrito";
        }
    }
}