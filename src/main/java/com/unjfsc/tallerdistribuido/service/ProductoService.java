package com.unjfsc.tallerdistribuido.service;

import com.unjfsc.tallerdistribuido.model.Producto;
import com.unjfsc.tallerdistribuido.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * CLASE DE SERVICIO: Orquesta todas las operaciones de negocio relacionadas con
 * los Productos. Es el componente central para la estrategia de caché,
 * asegurando que Redis y MySQL se mantengan sincronizados (eventualmente
 * consistentes).
 */
@Service
public class ProductoService {

	@Autowired
	private ProductoRepository productoRepository; // Inyección del repositorio para acceder a MySQL.

	/**
	 * Obtiene TODOS los productos. El resultado se cachea.
	 * 
	 * @Cacheable: Si la caché "productos_todos" tiene datos, los devuelve
	 *             directamente (desde una réplica de Redis). Si no, ejecuta el
	 *             método, consulta MySQL, y guarda el resultado en la caché para
	 *             futuras peticiones.
	 */
	@Cacheable("productos_todos")
	public List<Producto> findAll() {
		System.out.println("--- BUSCANDO TODOS LOS PRODUCTOS DESDE MYSQL (CACHE MISS) ---");
		return productoRepository.findAll();
	}

	/**
	 * Busca un producto por su ID. El resultado se cachea individualmente. key =
	 * "#id": La clave en Redis será dinámica, basada en el ID del producto (ej.
	 * "producto_id::5").
	 */
	@Cacheable(value = "producto_id", key = "#id")
	public Producto findById(Long id) {
		System.out.println("--- BUSCANDO PRODUCTO DESDE MYSQL (CACHE MISS) ID: " + id + " ---");
		return productoRepository.findById(id).orElse(null);
	}

	/**
	 * Guarda o actualiza un producto. Este método es CRÍTICO para la consistencia
	 * de la caché.
	 * 
	 * @Caching: Permite agrupar múltiples operaciones de caché. - @CachePut:
	 *           Siempre ejecuta el método (guarda en MySQL) y LUEGO actualiza la
	 *           caché "producto_id" con el nuevo objeto Producto devuelto. Asegura
	 *           que la caché del ítem individual esté siempre fresca.
	 *           - @CacheEvict: BORRA (invalida) las cachés que contienen listas, ya
	 *           que ahora están obsoletas. Esto fuerza a que la próxima vez que se
	 *           pidan, se recarguen desde MySQL.
	 */
	@Caching(put = { @CachePut(value = "producto_id", key = "#producto.id") }, evict = {
			@CacheEvict(value = "productos_todos", allEntries = true),
			@CacheEvict(value = "productos_filtrados", allEntries = true),
			@CacheEvict(value = "categorias", allEntries = true) })
	public Producto save(Producto producto) {
		System.out.println("--- GUARDANDO PRODUCTO EN MYSQL Y ACTUALIZANDO CACHÉ ---");
		return productoRepository.save(producto);
	}

	/**
	 * Borra un producto por su ID. También es crítico para la consistencia.
	 * 
	 * @CacheEvict: Invalida todas las cachés donde este producto podría haber
	 *              existido.
	 */
	@Caching(evict = { @CacheEvict(value = "productos_todos", allEntries = true),
			@CacheEvict(value = "productos_filtrados", allEntries = true),
			@CacheEvict(value = "producto_id", key = "#id"), @CacheEvict(value = "categorias", allEntries = true) })
	public void deleteById(Long id) {
		System.out.println("--- BORRANDO PRODUCTO DE MYSQL Y CACHÉ ---");
		productoRepository.deleteById(id);
	}

	/**
	 * Busca productos por nombre y/o categoría. El resultado se cachea. La clave de
	 * la caché es una combinación de los parámetros de búsqueda para que búsquedas
	 * idénticas se sirvan desde la caché.
	 */
	@Cacheable(value = "productos_filtrados", key = "'name=' + #name + '_category=' + #category")
	public List<Producto> search(String name, String category) {
		String searchName = (name == null) ? "" : name;
		String searchCategory = (category == null) ? "" : category;

		System.out.println(
				"--- BUSCANDO EN MYSQL (FILTRO - CACHE MISS) Nombre: " + searchName + ", Cat: " + searchCategory);
		return productoRepository.findByNameContainingIgnoreCaseAndCategoriaContainingIgnoreCase(searchName,
				searchCategory);
	}

	/**
	 * Obtiene la lista de categorías únicas. Se cachea para no repetir la consulta
	 * a la BD.
	 */
	@Cacheable("categorias")
	public Set<String> findDistinctCategorias() {
		System.out.println("--- BUSCANDO CATEGORÍAS ÚNICAS EN MYSQL (CACHE MISS) ---");
		return productoRepository.findDistinctCategorias();
	}
}