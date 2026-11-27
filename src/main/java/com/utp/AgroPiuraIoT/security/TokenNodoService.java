package com.utp.AgroPiuraIoT.security;

import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validación de tokens Bearer para la API de telemetría (A2), unificada con
 * los tokens BCrypt generados por {@link com.utp.AgroPiuraIoT.service.NodoTokenService} (A1).
 * Nunca escribe el token en texto plano en logs.
 */
@Service
public class TokenNodoService {

    private static final Logger log = LoggerFactory.getLogger(TokenNodoService.class);

    private final DispositivoIoTRepository dispositivos;
    private final PasswordEncoder passwordEncoder;

    public TokenNodoService(DispositivoIoTRepository dispositivos, PasswordEncoder passwordEncoder) {
        this.dispositivos = dispositivos;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Valida un token Bearer contra los hashes BCrypt almacenados en {@code dispositivo_iot}.
     * Distingue entre nodo válido activo, nodo inactivo (403) y token inválido (401).
     */
    @Transactional(readOnly = true)
    public ResultadoValidacion validar(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            return ResultadoValidacion.invalido();
        }
        String token = tokenPlano.trim();
        for (DispositivoIoT dispositivo : dispositivos.findByTokenHashIsNotNull()) {
            if (dispositivo.tieneToken() && passwordEncoder.matches(token, dispositivo.getTokenHash())) {
                if (!dispositivo.estaActivo()) {
                    log.warn("Intento de telemetría rechazado: nodo {} inactivo", dispositivo.getIdDispositivo());
                    return ResultadoValidacion.inactivo(dispositivo.getIdDispositivo());
                }
                return ResultadoValidacion.valido(dispositivo.getIdDispositivo());
            }
        }
        log.warn("Token Bearer rechazado en /api/telemetria");
        return ResultadoValidacion.invalido();
    }

    public enum EstadoValidacion {
        VALIDO,
        NODO_INACTIVO,
        INVALIDO
    }

    public record ResultadoValidacion(EstadoValidacion estado, Integer idDispositivo) {
        public static ResultadoValidacion valido(Integer idDispositivo) {
            return new ResultadoValidacion(EstadoValidacion.VALIDO, idDispositivo);
        }

        public static ResultadoValidacion inactivo(Integer idDispositivo) {
            return new ResultadoValidacion(EstadoValidacion.NODO_INACTIVO, idDispositivo);
        }

        public static ResultadoValidacion invalido() {
            return new ResultadoValidacion(EstadoValidacion.INVALIDO, null);
        }
    }
}
