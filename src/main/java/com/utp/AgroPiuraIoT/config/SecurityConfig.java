package com.utp.AgroPiuraIoT.config;

import com.utp.AgroPiuraIoT.security.TokenNodoAuthenticationFilter;
import com.utp.AgroPiuraIoT.security.TokenNodoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad en dos cadenas separadas:
 * <ol>
 *   <li>{@code /api/**} (@Order 1): stateless, sin CSRF ni redirección al login,
 *       autenticación por token Bearer de nodo ({@code ROLE_NODO}) con respuestas JSON (A2).</li>
 *   <li>Resto de rutas web (@Order 2): formulario con sesión, CSRF activo y autorización
 *       por cultivo con {@code @PreAuthorize("@permisos...")} (RF-07, C1).</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Publica eventos de éxito/fallo de autenticación para bloqueo temporal y auditoría (C1). */
    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    /** Cadena exclusiva para la API de telemetría IoT (/api/**), aislada de la sesión web. */
    @Bean
    @Order(1)
    SecurityFilterChain filtroApiTelemetria(HttpSecurity http,
                                            TokenNodoService tokenNodoService) throws Exception {
        TokenNodoAuthenticationFilter filtroToken = new TokenNodoAuthenticationFilter(tokenNodoService);
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/telemetria").hasRole("NODO")
                        .anyRequest().denyAll())
                .addFilterBefore(filtroToken, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.setCharacterEncoding("UTF-8");
                            res.getWriter().write(
                                    "{\"status\":\"error\",\"mensaje\":\"Token de nodo ausente o inválido\"}");
                        })
                        .accessDeniedHandler((req, res, deniedEx) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.setCharacterEncoding("UTF-8");
                            res.getWriter().write(
                                    "{\"status\":\"error\",\"mensaje\":\"Acceso denegado\"}");
                        }));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro",
                                "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
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
