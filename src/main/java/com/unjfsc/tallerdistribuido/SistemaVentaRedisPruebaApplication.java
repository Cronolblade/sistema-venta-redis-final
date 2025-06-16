package com.unjfsc.tallerdistribuido;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SistemaVentaRedisPruebaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaVentaRedisPruebaApplication.class, args);
	}

}
