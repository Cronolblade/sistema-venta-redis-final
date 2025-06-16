package com.unjfsc.tallerdistribuido.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.lettuce.core.ReadFrom;

/**
 * CLASE DE CONFIGURACIÓN (REPLICACIÓN): El cerebro de la estrategia de
 * replicación. Su responsabilidad es construir una conexión a Redis que sea
 * consciente de la topología Maestro/Réplica y que sepa cómo distribuir las
 * operaciones de lectura.
 */
@Configuration
public class RedisCacheConfig {

	// [REPLICACIÓN - PASO 1: Inyección de Configuración]
	// Se leen las propiedades de Sentinel desde application.properties.
	@Value("${spring.redis.sentinel.master}")
	private String sentinelMaster;

	@Value("${spring.redis.sentinel.nodes}")
	private List<String> sentinelNodes;

	@Value("${spring.redis.password}")
	private String redisPassword;

	/**
	 * Define la configuración para conectarse a Redis a través de Sentinel. Este
	 * bean le dice a la aplicación CÓMO encontrar el clúster.
	 */
	@Bean
	public RedisSentinelConfiguration redisSentinelConfiguration() {
		RedisSentinelConfiguration config = new RedisSentinelConfiguration();
		config.master(sentinelMaster);
		sentinelNodes.forEach(node -> {
			String[] parts = node.split(":");
			config.sentinel(parts[0], Integer.parseInt(parts[1]));
		});
		config.setPassword(redisPassword);
		return config;
	}

	/**
	 * [REPLICACIÓN - PASO 2: Definición del Comportamiento] Este bean define la
	 * regla de oro para la replicación. Le dice al cliente Lettuce que TODAS las
	 * operaciones de solo lectura deben preferentemente ir a un nodo de réplica.
	 */
	@Bean
	public LettuceClientConfiguration lettuceClientConfiguration() {
		// [CONCEPTO CLAVE]: ReadFrom.REPLICA_PREFERRED.
		// Es la instrucción directa para lograr la segregación de lecturas.
		return LettuceClientConfiguration.builder().readFrom(ReadFrom.REPLICA_PREFERRED).build();
	}

	/**
	 * [REPLICACIÓN - PASO 3: Creación de la Conexión] Este método une todo. Crea la
	 * factoría de conexiones final que será usada por toda la aplicación,
	 * combinando la información de DÓNDE está el clúster (Sentinel) con la regla de
	 * CÓMO usarlo (leer de las réplicas).
	 */
	@Bean
	@Primary // Marca esta factoría como la principal, evitando ambigüedades.
	public RedisConnectionFactory redisConnectionFactory(RedisSentinelConfiguration sentinelConfiguration,
			LettuceClientConfiguration clientConfiguration) {

		return new LettuceConnectionFactory(sentinelConfiguration, clientConfiguration);
	}
}