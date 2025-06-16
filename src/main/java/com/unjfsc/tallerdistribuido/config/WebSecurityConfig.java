package com.unjfsc.tallerdistribuido.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

	private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

	public WebSecurityConfig(CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
		this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
	}

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())

				.authorizeHttpRequests(authorize -> authorize
						// --- INICIO DE LA CORRECCIÓN ---
						// Añadimos /js/** a la lista de recursos públicos.
						.requestMatchers("/registro/**", "/css/**", "/js/**", "/Admin/**").permitAll()
						// --- FIN DE LA CORRECCIÓN ---

						.requestMatchers("/catalogo", "/carrito/**", "/favoritos/**").hasRole("USER")
						.requestMatchers(HttpMethod.GET, "/api/v1/productos/**").permitAll() // CAMBIO: Permitir a todos
																								// para el filtro
						.requestMatchers(HttpMethod.POST, "/api/v1/productos/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/productos/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/productos/**").hasRole("ADMIN")
						.requestMatchers("/ventas/**").hasRole("ADMIN").anyRequest().authenticated())
				.formLogin(
						form -> form.loginPage("/login").successHandler(customAuthenticationSuccessHandler).permitAll())
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());

		return http.build();
	}
}