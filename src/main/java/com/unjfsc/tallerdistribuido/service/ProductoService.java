package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.dto.UpdateNotification;
import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ProductoService {

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	// --- MÉTODOS DE BÚSQUEDA ---

	@Cacheable("productos_todos")
	public List<Producto> findAll() {
		System.out.println("--- BUSCANDO TODOS LOS PRODUCTOS DESDE MYSQL (CACHE MISS) ---");
		return productoRepository.findAll();
	}

	@Cacheable(value = "producto_id", key = "#id")
	public Producto findById(Long id) {
		System.out.println("--- BUSCANDO PRODUCTO DESDE MYSQL (CACHE MISS) ID: " + id + " ---");
		return productoRepository.findById(id).orElse(null);
	}

	@Cacheable(value = "productos_filtrados", key = "'name=' + #name + '_category=' + #category")
	public List<Producto> search(String name, String category) {
		String searchName = (name == null) ? "" : name;
		String searchCategory = (category == null) ? "" : category;
		System.out.println(
				"--- BUSCANDO EN MYSQL (FILTRO - CACHE MISS) Nombre: " + searchName + ", Cat: " + searchCategory);
		return productoRepository.findByNameContainingIgnoreCaseAndCategoriaContainingIgnoreCase(searchName,
				searchCategory);
	}

	@Cacheable("categorias")
	public Set<String> findDistinctCategorias() {
		System.out.println("--- BUSCANDO CATEGORÍAS ÚNICAS EN MYSQL (CACHE MISS) ---");
		return productoRepository.findDistinctCategorias();
	}

	// --- MÉTODOS DE MODIFICACIÓN Y NOTIFICACIÓN ---

	@Caching(put = { @CachePut(value = "producto_id", key = "#producto.id") }, evict = {
			@CacheEvict(value = "productos_todos", allEntries = true),
			@CacheEvict(value = "productos_filtrados", allEntries = true),
			@CacheEvict(value = "categorias", allEntries = true) })
	public Producto save(Producto producto) {
		System.out.println("--- GUARDANDO/ACTUALIZANDO PRODUCTO EN MYSQL Y CACHÉ ---");
		return productoRepository.save(producto);
	}

	@Caching(evict = { @CacheEvict(value = "productos_todos", allEntries = true),
			@CacheEvict(value = "productos_filtrados", allEntries = true),
			@CacheEvict(value = "producto_id", key = "#id"), @CacheEvict(value = "categorias", allEntries = true) })
	public void deleteById(Long id) {
		System.out.println("--- BORRANDO PRODUCTO DE MYSQL Y CACHÉ ---");
		productoRepository.deleteById(id);
	}

	/**
	 * Método público y explícito para enviar notificaciones a todos los clientes.
	 * Obtiene la lista más fresca de productos y la envía por WebSocket.
	 */
	public void notifyClientsOfUpdate() {
		// ================================================================
		// ===================== CORRECCIÓN IMPORTANTE ====================
		// Llamamos directamente al método del repositorio para evitar problemas de
		// auto-invocación.
		// Esto consulta la base de datos y obtiene la lista más fresca.
		List<Producto> productosActualizados = productoRepository
				.findByNameContainingIgnoreCaseAndCategoriaContainingIgnoreCase("", "");
		// ================================================================

		UpdateNotification notification = new UpdateNotification(productosActualizados);
		messagingTemplate.convertAndSend("/topic/productos", notification);
		System.out.println("--- ENVIANDO NOTIFICACIÓN WEBSOCKET COMPLETA A /topic/productos ---");
	}
}