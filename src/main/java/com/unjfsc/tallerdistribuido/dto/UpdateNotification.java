package com.unjfsc.tallerdistribuido.dto;

import com.unjfsc.tallerdistribuido.model.Producto;
import java.util.List;

/**
 * DTO (Data Transfer Object) para las notificaciones de WebSocket. Encapsula la
 * lista de productos actualizada que se enviará a los clientes.
 */
public class UpdateNotification {
	private String type = "PRODUCT_UPDATE";
	private List<Producto> productos;

	public UpdateNotification(List<Producto> productos) {
		this.productos = productos;
	}

	// Getters y Setters necesarios para la serialización a JSON.
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Producto> getProductos() {
		return productos;
	}

	public void setProductos(List<Producto> productos) {
		this.productos = productos;
	}
}