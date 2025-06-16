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

@Service
public class FavoritosService {

	private final RedisTemplate<String, String> redisTemplate;
	private final ProductoRepository productoRepository;

	@Autowired
	public FavoritosService(RedisTemplate<String, String> redisTemplate, ProductoRepository productoRepository) {
		this.redisTemplate = redisTemplate;
		this.productoRepository = productoRepository;
	}

	private String getFavoritosKey(String username) {
		return "favoritos:" + username;
	}

	public void agregarFavorito(String username, Long productoId) {
		redisTemplate.opsForSet().add(getFavoritosKey(username), String.valueOf(productoId));
	}

	public void eliminarFavorito(String username, Long productoId) {
		redisTemplate.opsForSet().remove(getFavoritosKey(username), String.valueOf(productoId));
	}

	public Set<String> getProductosFavoritosIds(String username) {
		Set<String> members = redisTemplate.opsForSet().members(getFavoritosKey(username));
		return members != null ? members : Set.of();
	}

	public Set<Producto> getProductosFavoritos(String username) {
		Set<String> productoIdsStr = getProductosFavoritosIds(username);

		if (productoIdsStr.isEmpty()) {
			return Set.of();
		}

		Set<Long> longProductoIds = productoIdsStr.stream().map(Long::valueOf).collect(Collectors.toSet());

		List<Producto> productos = productoRepository.findAllById(longProductoIds);

		return new HashSet<>(productos);
	}

	public Long getFavoritosCount(String username) {
		String favoritosKey = getFavoritosKey(username);
		Long size = redisTemplate.opsForSet().size(favoritosKey);
		return size != null ? size : 0L;
	}

	/**
	 * Añade o quita un producto de la lista de favoritos. Si el producto ya es
	 * favorito, lo quita. Si no, lo añade. Es un método "toggle".
	 * 
	 * @return true si el producto fue añadido, false si fue quitado.
	 */
	public boolean toggleFavorito(String username, Long productoId) {
		String favoritosKey = getFavoritosKey(username);
		String productoIdStr = String.valueOf(productoId);

		// Comprueba si el producto ya está en el set de favoritos.
		Boolean esMiembro = redisTemplate.opsForSet().isMember(favoritosKey, productoIdStr);

		if (Boolean.TRUE.equals(esMiembro)) {
			// Si ya es favorito, lo eliminamos.
			eliminarFavorito(username, productoId);
			return false; // Indica que se quitó.
		} else {
			// Si no es favorito, lo añadimos.
			agregarFavorito(username, productoId);
			return true; // Indica que se añadió.
		}
	}
}