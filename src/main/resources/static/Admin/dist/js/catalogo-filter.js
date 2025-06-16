document.addEventListener('DOMContentLoaded', function() {
	// Referencias a los elementos del DOM
	const searchBox = document.getElementById('search-box');
	const categoryFilter = document.getElementById('category-filter');
	const productContainer = document.getElementById('product-list-container');
	let searchTimeout;

	// Leemos los IDs de los productos favoritos desde el atributo data-* del body.
	const favoritosIds = new Set(JSON.parse(document.body.getAttribute('data-favoritos-ids') || '[]'));

	// --- FUNCIÓN DE RENDERIZADO ---
	// Toma una lista de productos en formato JSON y construye el HTML correspondiente.
	function renderProducts(products) {
		productContainer.innerHTML = '';

		if (products.length === 0) {
			productContainer.innerHTML = `
                <div class="col-12">
                    <div class="callout callout-info">
                        <h5>No se encontraron productos</h5>
                        <p>Intenta con otros criterios de búsqueda o revisa todas las categorías.</p>
                    </div>
                </div>`;
			return;
		}

		products.forEach(producto => {
			const esFavorito = favoritosIds.has(String(producto.id));
			const isSoldOut = producto.stock === 0;

			const card = document.createElement('div');
			card.className = 'col-12 col-sm-6 col-md-4 d-flex align-items-stretch';
			card.innerHTML = `
                <div class="card bg-light d-flex flex-fill ${isSoldOut ? 'card-sold-out' : ''}">
                    <div class="card-header text-muted border-bottom-0">${producto.categoria}</div>
                    <div class="card-body pt-0">
                        <div class="row">
                            <div class="col-7">
                                <h2 class="lead"><b>${producto.name}</b></h2>
                                <p class="text-muted text-sm"><b>Stock: </b><span>${producto.stock}</span></p>
                                <ul class="ml-4 mb-0 fa-ul text-muted">
                                    <li class="small"><span class="fa-li"><i class="fas fa-dollar-sign"></i></span> Precio: S/ ${producto.precio.toFixed(2)}</li>
                                </ul>
                            </div>
                            <div class="col-5 text-center">
                                <img src="${producto.imageUrl || '/Admin/dist/assets/img/productos/default-product.png'}" 
                                     alt="product-image" class="img-circle img-fluid"
                                     style="height: 100px; width: 100px; object-fit: cover;">
                            </div>
                        </div>
                    </div>
                    <div class="card-footer">
                        <div class="text-right">
                            <form action="/catalogo/toggle-favorito/${producto.id}" method="post" class="d-inline-block mr-1">
                                <button type="submit" class="btn btn-sm ${esFavorito ? 'btn-danger' : 'btn-outline-danger'}" title="${esFavorito ? 'Quitar de Favoritos' : 'Añadir a Favoritos'}">
                                    <i class="${esFavorito ? 'fas' : 'far'} fa-heart"></i>
                                </button>
                            </form>
                            <form action="/carrito/agregar/${producto.id}" method="post" class="d-inline-flex align-items-center">
                                <input type="number" name="cantidad" value="1" min="1" max="${producto.stock}" 
                                       oninput="validateQuantity(this)"
                                       class="form-control form-control-sm mr-2" style="width: 70px;" ${isSoldOut ? 'disabled' : ''}>
                                <button type="submit" class="btn btn-sm btn-primary" ${isSoldOut ? 'disabled' : ''}>
                                    <i class="fas fa-shopping-cart"></i> Añadir
                                </button>
                            </form>
                        </div>
                    </div>
                </div>`;
			productContainer.appendChild(card);
		});
	}

	// --- FUNCIÓN DE BÚSQUEDA ---
	// Llama a la API REST para obtener la lista de productos filtrada.
	async function fetchAndRenderProducts() {
		const searchTerm = searchBox.value;
		const category = categoryFilter.value;

		const url = new URL('/api/v1/productos/search', window.location.origin);
		if (searchTerm) url.searchParams.append('name', searchTerm);
		if (category) url.searchParams.append('category', category);

		try {
			const response = await fetch(url);
			if (!response.ok) throw new Error('Error en la búsqueda');
			const products = await response.json();
			renderProducts(products);
		} catch (error) {
			productContainer.innerHTML = '<div class="col-12"><p class="text-danger">Error al cargar los productos.</p></div>';
			console.error('Error:', error);
		}
	}

	// --- LÓGICA DE WEBSOCKET ---
	function connectWebSocket() {
		const socket = new SockJS('/ws');
		const stompClient = Stomp.over(socket);
		stompClient.debug = null;

		stompClient.connect({}, function(frame) {
			console.log('Conectado al WebSocket: ' + frame);

			stompClient.subscribe('/topic/productos', function(message) {
				const notification = JSON.parse(message.body);
				console.log('Notificación recibida:', notification);

				if (notification.type === 'PRODUCT_UPDATE' && notification.productos) {
					console.log("Actualizando la vista con los datos del WebSocket.");
					// Usamos la lista que viene en el mensaje, no hacemos otro fetch.
					renderProducts(notification.productos);
				}
			});
		}, function(error) {
			console.error('Error de WebSocket, intentando reconectar en 5 segundos...', error);
			setTimeout(connectWebSocket, 5000);
		});
	}

	// --- EJECUCIÓN INICIAL ---
	searchBox.addEventListener('input', () => {
		clearTimeout(searchTimeout);
		searchTimeout = setTimeout(fetchAndRenderProducts, 300);
	});
	categoryFilter.addEventListener('change', fetchAndRenderProducts);

	fetchAndRenderProducts(); // Carga inicial de productos.
	connectWebSocket();     // <-- ¡¡LÍNEA CLAVE DESCOMENTADA!! Inicia la conexión WebSocket.
});