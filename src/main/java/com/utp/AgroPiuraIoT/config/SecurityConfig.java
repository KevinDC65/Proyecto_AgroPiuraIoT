package com.utp.AgroPiuraIoT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autenticación por formulario con sesión y CSRF (activo por defecto).
 * La autorización por cultivo (RF-07) se declara en cada controlador con
 * {@code @PreAuthorize("@permisos...")}: el rol depende del cultivo, no del
 * usuario, así que no puede expresarse con reglas por URL.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro",
                                "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        // API de telemetría cerrada hasta autenticar cada nodo con su token
                        // (issues A1 y A2 de docs/11-plan-de-sprints.md). Abierta, cualquiera
                        // podría escribir lecturas en nodos de cultivos ajenos.
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll());
        return http.build();
    }
}
