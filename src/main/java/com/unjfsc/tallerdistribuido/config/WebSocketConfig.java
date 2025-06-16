package com.unjfsc.tallerdistribuido.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
	}

	/**
	 * Registra el endpoint de WebSocket, ahora con una configuración explícita para
	 * permitir orígenes cruzados (CORS), incluyendo el de ngrok.
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// --- CAMBIO IMPORTANTE ---
		registry.addEndpoint("/ws")
				// Le decimos explícitamente qué orígenes están permitidos.
				// NO usamos "*" para evitar el error con las credenciales.
				.setAllowedOrigins("http://localhost:8080",
						// Asumimos que ngrok usa https
						"https://*.ngrok-free.app")
				.withSockJS();
	}
}