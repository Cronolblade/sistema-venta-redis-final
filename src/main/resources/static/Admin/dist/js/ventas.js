// Cargar todos los productos al iniciar la página
document.addEventListener('DOMContentLoaded', () => {
	loadProducts();
});

// --- RUTA BASE DE LA API ---
const API_BASE_URL = '/api/v1/productos';

// --- LÓGICA PARA CREAR PRODUCTOS ---
document.getElementById('create-form').addEventListener('submit', async (e) => {
	e.preventDefault();
	const product = {
		name: document.getElementById('name').value,
		precio: parseFloat(document.getElementById('price').value),
		stock: parseInt(document.getElementById('stock').value),
		categoria: document.getElementById('categoria').value,
		imageUrl: document.getElementById('imageUrl').value
	};

	try {
		const response = await fetch(API_BASE_URL, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(product)
		});

		if (response.status === 403) {
			alert("Acceso denegado. Solo los administradores pueden crear productos.");
			return;
		}
		if (!response.ok) throw new Error('Error al crear el producto.');

		const createdProduct = await response.json();
		alert(`Producto '${createdProduct.name}' creado.`);
		document.getElementById('create-form').reset();
		loadProducts();
	} catch (error) {
		console.error("Error en creación:", error);
		alert("Ocurrió un error al crear el producto.");
	}
});


// --- LÓGICA PARA LEER (LISTAR) PRODUCTOS ---
async function loadProducts() {
	try {
		const response = await fetch(API_BASE_URL);
		if (!response.ok) throw new Error('Error al cargar los productos.');

		const products = await response.json();
		const tableBody = document.getElementById('products-table-body');
		tableBody.innerHTML = '';

		if (products.length === 0) {
			tableBody.innerHTML = '<tr><td colspan="5" class="text-center">No hay productos para mostrar.</td></tr>';
			return;
		}

		products.forEach(product => {
			const row = `
                <tr>
                    <td>${product.name}</td>
                    <td>S/ ${product.precio.toFixed(2)}</td>
                    <td>${product.stock}</td>
                    <td>${product.categoria}</td>
                    <td>
                        <button class="btn btn-sm btn-info" onclick='showEditModal(${JSON.stringify(product)})'>Editar</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteProduct(${product.id}, '${product.name}')">Eliminar</button>
                    </td>
                </tr>
            `;
			tableBody.innerHTML += row;
		});
	} catch (error) {
		console.error("Error en carga:", error);
		alert("Ocurrió un error al cargar la lista de productos.");
	}
}


// --- LÓGICA PARA ACTUALIZAR PRODUCTOS ---
function showEditModal(product) {
	document.getElementById('edit-id').value = product.id;
	document.getElementById('edit-name').value = product.name;
	document.getElementById('edit-price').value = product.precio;
	document.getElementById('edit-stock').value = product.stock;
	document.getElementById('edit-categoria').value = product.categoria;
	document.getElementById('edit-imageUrl').value = product.imageUrl;
	$('#editProductModal').modal('show');
}

document.getElementById('edit-form').addEventListener('submit', async (e) => {
	e.preventDefault();
	const id = document.getElementById('edit-id').value;
	const product = {
		name: document.getElementById('edit-name').value,
		precio: parseFloat(document.getElementById('edit-price').value),
		stock: parseInt(document.getElementById('edit-stock').value),
		categoria: document.getElementById('edit-categoria').value,
		imageUrl: document.getElementById('edit-imageUrl').value
	};

	try {
		const response = await fetch(`${API_BASE_URL}/${id}`, {
			method: 'PUT',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(product)
		});

		if (response.status === 403) {
			alert("Acceso denegado. Solo los administradores pueden actualizar productos.");
			return;
		}
		if (!response.ok) throw new Error('Error al actualizar el producto.');

		$('#editProductModal').modal('hide');
		alert(`Producto '${product.name}' actualizado.`);
		loadProducts();
	} catch (error) {
		console.error("Error en actualización:", error);
		alert("Ocurrió un error al actualizar el producto.");
	}
});


// --- LÓGICA PARA ELIMINAR PRODUCTOS ---
async function deleteProduct(id, name) {
	if (!confirm(`¿Estás seguro de que quieres eliminar el producto '${name}'?`)) return;

	try {
		const response = await fetch(`${API_BASE_URL}/${id}`, {
			method: 'DELETE'
		});

		if (response.status === 403) {
			alert("Acceso denegado. Solo los administradores pueden eliminar productos.");
			return;
		}
		if (!response.ok) throw new Error('Error al eliminar el producto.');

		alert(`Producto '${name}' eliminado.`);
		loadProducts();
	} catch (error) {
		console.error("Error en eliminación:", error);
		alert("Ocurrió un error al eliminar el producto.");
	}
}