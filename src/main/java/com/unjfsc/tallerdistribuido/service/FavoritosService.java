package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CLASE DE SERVICIO: Encapsula toda la lógica de negocio para la gestión de
 * favoritos. Actúa como intermediario entre los controladores y las fuentes de
 * datos (Redis y MySQL). Su responsabilidad es manejar cómo se almacenan y
 * recuperan los favoritos.
 */
@Service // [CONCEPTO CLAVE]: Anotación de Servicio. Marca esta clase como un componente
			// de lógica de negocio.
public class FavoritosService {

	// [CONCEPTO CLAVE]: RedisTemplate. Es la herramienta principal de Spring para
	// interactuar con Redis.
	// Está configurado para trabajar con claves de tipo String y valores de tipo
	// String en este caso.
	private final RedisTemplate<String, String> redisTemplate;

	// [CONCEPTO CLAVE]: Repositorio JPA. La puerta de entrada para interactuar con
	// la base de datos MySQL.
	// Se necesita para obtener los detalles completos de los productos (nombre,
	// precio, etc.) a partir de sus IDs.
	private final ProductoRepository productoRepository;

	/**
	 * CONSTRUCTOR: Utiliza la inyección de dependencias a través del constructor.
	 * Es una buena práctica porque asegura que el servicio no se puede crear sin
	 * sus dependencias esenciales.
	 * 
	 * @param redisTemplate      Spring inyecta el bean RedisTemplate configurado
	 *                           automáticamente.
	 * @param productoRepository Spring inyecta el repositorio JPA para la entidad
	 *                           Producto.
	 */
	@Autowired
	public FavoritosService(RedisTemplate<String, String> redisTemplate, ProductoRepository productoRepository) {
		this.redisTemplate = redisTemplate;
		this.productoRepository = productoRepository;
	}

	/**
	 * Método de utilidad privado para generar una clave única de Redis para la
	 * lista de favoritos de cada usuario. Ej: "favoritos:eduardo@gmail.com"
	 */
	private String getFavoritosKey(String username) {
		return "favoritos:" + username;
	}

	// --- MÉTODOS DE OPERACIONES BÁSICAS (CRUD en Redis) ---

	public void agregarFavorito(String username, Long productoId) {
		// [COMANDO REDIS]: SADD. Añade un miembro a un Set. Si ya existe, no hace nada.
		// Los Sets son ideales para listas de favoritos porque automáticamente evitan
		// duplicados.
		redisTemplate.opsForSet().add(getFavoritosKey(username), String.valueOf(productoId));
	}

	public void eliminarFavorito(String username, Long productoId) {
		// [COMANDO REDIS]: SREM. Elimina un miembro de un Set.
		redisTemplate.opsForSet().remove(getFavoritosKey(username), String.valueOf(productoId));
	}

	// --- MÉTODOS DE CONSULTA ---

	/**
	 * Obtiene únicamente los IDs (como Strings) de los productos favoritos de un
	 * usuario. Es muy rápido porque solo consulta Redis.
	 */
	public Set<String> getProductosFavoritosIds(String username) {
		// [COMANDO REDIS]: SMEMBERS. Devuelve todos los miembros de un Set.
		Set<String> members = redisTemplate.opsForSet().members(getFavoritosKey(username));
		// Se añade una comprobación para evitar NullPointerException si la clave no
		// existe.
		return members != null ? members : Set.of();
	}

	/**
	 * Obtiene los objetos Producto completos de la lista de favoritos. [LÓGICA
	 * HÍBRIDA]: Este método demuestra la arquitectura del sistema. 1. Consulta
	 * RÁPIDA a Redis para obtener los IDs. 2. Consulta ÚNICA y EFICIENTE a MySQL
	 * para obtener los detalles.
	 */
	public Set<Producto> getProductosFavoritos(String username) {
		// Paso 1: Obtener los IDs de favoritos desde Redis.
		Set<String> productoIdsStr = getProductosFavoritosIds(username);

		// Si no hay favoritos, devolvemos un conjunto vacío para evitar consultas
		// innecesarias a la BD.
		if (productoIdsStr.isEmpty()) {
			return Set.of();
		}

		// Paso 2: Convertir los IDs de String a Long para usarlos con JPA.
		Set<Long> longProductoIds = productoIdsStr.stream().map(Long::valueOf).collect(Collectors.toSet());

		// Paso 3: Realizar una única consulta a MySQL con todos los IDs.
		// [CONCEPTO CLAVE]: `findAllById` es mucho más eficiente que un bucle con
		// `findById` (evita el problema N+1).
		List<Producto> productos = productoRepository.findAllById(longProductoIds);

		// Devolvemos el resultado como un Set.
		return new HashSet<>(productos);
	}

	/**
	 * Cuenta el número de productos distintos en la lista de favoritos de un
	 * usuario.
	 */
	public Long getFavoritosCount(String username) {
		String favoritosKey = getFavoritosKey(username);
		// [COMANDO REDIS]: SCARD. Devuelve la cardinalidad (número de elementos) de un
		// Set. Es una operación O(1), instantánea.
		Long size = redisTemplate.opsForSet().size(favoritosKey);
		return size != null ? size : 0L;
	}

	/**
	 * Añade o quita un producto de la lista de favoritos (lógica de "toggle").
	 */
	public boolean toggleFavorito(String username, Long productoId) {
		String favoritosKey = getFavoritosKey(username);
		String productoIdStr = String.valueOf(productoId);

		// [COMANDO REDIS]: SISMEMBER. Comprueba si un valor es miembro de un Set. Es
		// una operación O(1).
		Boolean esMiembro = redisTemplate.opsForSet().isMember(favoritosKey, productoIdStr);

		if (Boolean.TRUE.equals(esMiembro)) {
			// Si ya es favorito, se elimina.
			eliminarFavorito(username, productoId);
			return false; // Se quitó.
		} else {
			// Si no es favorito, se añade.
			agregarFavorito(username, productoId);
			return true; // Se añadió.
		}
	}
}