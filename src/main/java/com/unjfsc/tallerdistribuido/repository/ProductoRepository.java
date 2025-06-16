package com.unjfsc.tallerdistribuido.repository;

import com.unjfsc.tallerdistribuido.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	/**
	 * Busca productos donde el nombre contenga el término de búsqueda y la
	 * categoría contenga el término de categoría, ambos ignorando
	 * mayúsculas/minúsculas. Si un término es una cadena vacía, actúa como un
	 * comodín para ese campo.
	 */
	List<Producto> findByNameContainingIgnoreCaseAndCategoriaContainingIgnoreCase(String name, String categoria);

	/**
	 * Obtiene una lista de todas las categorías de productos únicas y no nulas. Se
	 * usará para poblar el dropdown de filtros.
	 */
	@Query("SELECT DISTINCT p.categoria FROM Producto p WHERE p.categoria IS NOT NULL AND p.categoria <> ''")
	Set<String> findDistinctCategorias();
}