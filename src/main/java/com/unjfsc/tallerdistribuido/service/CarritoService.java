package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CarritoService {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private ProductoService productoService;

	private String getCartKey(String username) {
		return "cart:" + username;
	}

	public void agregarProducto(String username, Long productoId, int cantidad) {
		Producto producto = productoRepository.findById(productoId)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

		if (producto.getStock() < cantidad) {
			throw new InsufficientStockException("Stock insuficiente para el producto: " + producto.getName());
		}

		redisTemplate.opsForHash().increment(getCartKey(username), String.valueOf(productoId), cantidad);
	}

	public void eliminarProducto(String username, Long productoId) {
		redisTemplate.opsForHash().delete(getCartKey(username), String.valueOf(productoId));
	}

	public Map<Producto, Integer> getProductosEnCarrito(String username) {
		Map<Object, Object> items = redisTemplate.opsForHash().entries(getCartKey(username));

		if (items.isEmpty()) {
			return Map.of();
		}

		List<Long> productoIds = items.keySet().stream().map(id -> Long.valueOf((String) id))
				.collect(Collectors.toList());

		Map<Long, Producto> productosEncontrados = productoRepository.findAllById(productoIds).stream()
				.collect(Collectors.toMap(Producto::getId, producto -> producto));

		return items.entrySet().stream().map(entry -> {
			Long productoId = Long.valueOf((String) entry.getKey());
			Producto producto = productosEncontrados.get(productoId);
			if (producto != null) {
				return Map.entry(producto, Integer.parseInt((String) entry.getValue()));
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Transactional
	public void realizarCompra(String username) {
		Map<Producto, Integer> productosEnCarrito = getProductosEnCarrito(username);

		if (productosEnCarrito.isEmpty()) {
			throw new IllegalStateException("El carrito está vacío.");
		}

		for (Map.Entry<Producto, Integer> entry : productosEnCarrito.entrySet()) {
			Producto producto = entry.getKey();
			Integer cantidadPedida = entry.getValue();

			Producto productoEnDB = productoRepository.findById(producto.getId())
					.orElseThrow(() -> new IllegalStateException("Producto no encontrado durante la compra"));

			if (productoEnDB.getStock() < cantidadPedida) {
				throw new InsufficientStockException("Stock insuficiente para '" + producto.getName() + "'");
			}

			productoEnDB.setStock(productoEnDB.getStock() - cantidadPedida);

			// ================================================================
			// ===================== CORRECCIÓN IMPORTANTE ====================
			// Llamamos al único método save de ProductoService. Este método se
			// encarga de la BD y la caché. La notificación la hará el controlador.
			productoService.save(productoEnDB);
			// ================================================================
		}

		redisTemplate.delete(getCartKey(username));
	}

	public Long getCartItemCount(String username) {
		return redisTemplate.opsForHash().size(getCartKey(username));
	}
}