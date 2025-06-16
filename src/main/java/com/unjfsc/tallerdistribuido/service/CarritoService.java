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

/**
 * CLASE DE SERVICIO: Gestiona la lógica de negocio del carrito de compras.
 * Utiliza Redis como almacenamiento principal para el carrito, ya que es un
 * dato volátil y específico de la sesión de un usuario.
 */
@Service
public class CarritoService {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private ProductoService productoService;

	/**
	 * Genera la clave única para el Hash de Redis que almacenará el carrito de un
	 * usuario.
	 */
	private String getCartKey(String username) {
		return "cart:" + username;
	}

	/**
	 * Añade un producto al carrito. Utiliza un Hash de Redis. Primero, valida la
	 * existencia y el stock del producto contra MySQL.
	 */
	public void agregarProducto(String username, Long productoId, int cantidad) {
		Producto producto = productoRepository.findById(productoId)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

		if (producto.getStock() < cantidad) {
			throw new InsufficientStockException("Stock insuficiente para el producto: " + producto.getName());
		}

		// COMANDO REDIS (HINCRBY): Incrementa el valor del campo (productoId) en el
		// hash por la cantidad.
		// Es una operación atómica y muy eficiente.
		redisTemplate.opsForHash().increment(getCartKey(username), String.valueOf(productoId), cantidad);
	}

	/**
	 * Elimina un producto del carrito.
	 */
	public void eliminarProducto(String username, Long productoId) {
		// COMANDO REDIS (HDEL): Elimina un campo del hash.
		redisTemplate.opsForHash().delete(getCartKey(username), String.valueOf(productoId));
	}

	/**
	 * Obtiene el contenido completo del carrito, combinando los datos de Redis y
	 * MySQL. Se optimiza para evitar el problema N+1.
	 */
	public Map<Producto, Integer> getProductosEnCarrito(String username) {
		// COMANDO REDIS (HGETALL): Obtiene todos los pares campo-valor del hash del
		// carrito.
		Map<Object, Object> items = redisTemplate.opsForHash().entries(getCartKey(username));

		if (items.isEmpty()) {
			return Map.of();
		}

		// Se obtienen todos los IDs de una vez.
		List<Long> productoIds = items.keySet().stream().map(id -> Long.valueOf((String) id))
				.collect(Collectors.toList());

		// Se hace una única consulta a MySQL para traer todos los detalles de los
		// productos.
		Map<Long, Producto> productosEncontrados = productoRepository.findAllById(productoIds).stream()
				.collect(Collectors.toMap(Producto::getId, producto -> producto));

		// Se ensambla el mapa final en memoria.
		return items.entrySet().stream().map(entry -> {
			Long productoId = Long.valueOf((String) entry.getKey());
			Producto producto = productosEncontrados.get(productoId);
			if (producto != null) {
				return Map.entry(producto, Integer.parseInt((String) entry.getValue()));
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * Procesa la compra. Es una operación transaccional para garantizar la
	 * integridad de los datos.
	 */
	@Transactional
	public void realizarCompra(String username) {
		Map<Producto, Integer> productosEnCarrito = getProductosEnCarrito(username);

		if (productosEnCarrito.isEmpty()) {
			throw new IllegalStateException("El carrito está vacío.");
		}

		// El bucle se ejecuta dentro de una transacción de base de datos.
		for (Map.Entry<Producto, Integer> entry : productosEnCarrito.entrySet()) {
			Producto producto = entry.getKey();
			Integer cantidadPedida = entry.getValue();

			Producto productoEnDB = productoRepository.findById(producto.getId())
					.orElseThrow(() -> new IllegalStateException("Producto no encontrado durante la compra"));

			if (productoEnDB.getStock() < cantidadPedida) {
				throw new InsufficientStockException("Stock insuficiente para '" + producto.getName() + "'");
			}

			// Se descuenta el stock del objeto en memoria.
			productoEnDB.setStock(productoEnDB.getStock() - cantidadPedida);

			// Se guarda el producto a través de ProductoService. Esto ejecuta el UPDATE en
			// MySQL
			// y, crucialmente, invalida las cachés de Redis relevantes.
			productoService.save(productoEnDB);
		}

		// Si toda la transacción de base de datos fue exitosa, se elimina el carrito de
		// Redis.
		redisTemplate.delete(getCartKey(username));
	}

	/**
	 * Cuenta el número de ítems distintos en el carrito.
	 */
	public Long getCartItemCount(String username) {
		// COMANDO REDIS (HLEN): Operación O(1) para contar los campos de un hash.
		return redisTemplate.opsForHash().size(getCartKey(username));
	}
}