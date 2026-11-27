package com.utp.AgroPiuraIoT.security;

import com.utp.AgroPiuraIoT.security.TokenNodoService.EstadoValidacion;
import com.utp.AgroPiuraIoT.security.TokenNodoService.ResultadoValidacion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de autenticación stateless por token Bearer exclusivo para {@code /api/**}.
 * Se instancia en {@code SecurityConfig} (no como bean global de servlet) para evitar
 * interferir con la cadena web de sesiones y CSRF.
 */
public class TokenNodoAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenNodoService tokenNodoService;

    public TokenNodoAuthenticationFilter(TokenNodoService tokenNodoService) {
        this.tokenNodoService = tokenNodoService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            // La API de nodos opera fuera de la sesión web: sin Bearer no hay autenticación válida.
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length()).trim();
        ResultadoValidacion resultado = tokenNodoService.validar(token);

        if (resultado.estado() == EstadoValidacion.VALIDO) {
            var auth = new UsernamePasswordAuthenticationToken(
                    resultado.idDispositivo(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_NODO")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        if (resultado.estado() == EstadoValidacion.NODO_INACTIVO) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":\"error\",\"mensaje\":\"El nodo se encuentra inactivo\"}");
            return;
        }

        // Token inválido: continúa sin autenticación para que el AuthenticationEntryPoint responda 401 JSON.
        filterChain.doFilter(request, response);
    }
}
