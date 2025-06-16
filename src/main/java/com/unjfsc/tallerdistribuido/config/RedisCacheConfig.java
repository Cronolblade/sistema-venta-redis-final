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

@Configuration
public class RedisCacheConfig {

	// --- Inyectamos los valores directamente desde application.properties ---
	@Value("${spring.redis.sentinel.master}")
	private String sentinelMaster;

	@Value("${spring.redis.sentinel.nodes}")
	private List<String> sentinelNodes;

	@Value("${spring.redis.password}")
	private String redisPassword;

	/**
	 * Define explícitamente la configuración para conectarse a Redis a través de
	 * Sentinel. Este bean se creará usando los valores que inyectamos arriba.
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
	 * Define el COMPORTAMIENTO del cliente Lettuce.
	 */
	@Bean
	public LettuceClientConfiguration lettuceClientConfiguration() {
		return LettuceClientConfiguration.builder().readFrom(ReadFrom.REPLICA_PREFERRED).build();
	}

	/**
	 * Crea la factoría de conexiones principal. Es el corazón de la conexión. Toma
	 * los dos beans de configuración que definimos arriba.
	 */
	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory(RedisSentinelConfiguration sentinelConfiguration,
			LettuceClientConfiguration clientConfiguration) {

		return new LettuceConnectionFactory(sentinelConfiguration, clientConfiguration);
	}
}