package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token con el que un nodo ESP32 se identifica ante la API de telemetría (A1).
 * <p>
 * Reglas: el token se genera con {@link SecureRandom}, se guarda solo su hash
 * BCrypt (el mismo {@link PasswordEncoder} de las contraseñas) y el texto plano se
 * devuelve una única vez para mostrárselo al administrador. Nunca se escribe en
 * logs. La autenticación HTTP que lo usa la implementa A2.
 */
@Service
public class NodoTokenService {

    private static final Logger log = LoggerFactory.getLogger(NodoTokenService.class);

    /** 32 bytes = 256 bits de azar; en Base64 URL son 43 caracteres (BCrypt admite hasta 72). */
    private static final int BYTES_TOKEN = 32;

    private final SecureRandom azar = new SecureRandom();
    private final PasswordEncoder encoder;
    private final DispositivoIoTRepository dispositivos;

    public NodoTokenService(PasswordEncoder encoder, DispositivoIoTRepository dispositivos) {
        this.encoder = encoder;
        this.dispositivos = dispositivos;
    }

    /**
     * Genera un token nuevo para el nodo, guarda su hash y devuelve el texto plano.
     * Si el nodo ya tenía token, el anterior deja de ser válido.
     */
    @Transactional
    public String asignarToken(DispositivoIoT nodo) {
        String token = generarToken();
        nodo.setTokenHash(encoder.encode(token));
        dispositivos.save(nodo);
        log.info("Token asignado al nodo {}", nodo.getIdDispositivo());
        return token;
    }

    /**
     * Comprueba que el token pertenezca al nodo indicado. Devuelve {@code false}
     * (sin dar más detalle) si el nodo no existe, no tiene token o el token no
     * coincide, para no revelar qué nodos existen.
     */
    @Transactional(readOnly = true)
    public boolean verificar(Integer idNodo, String token) {
        if (idNodo == null || token == null || token.isBlank()) {
            return false;
        }
        boolean valido = dispositivos.findById(idNodo)
                .filter(DispositivoIoT::tieneToken)
                .map(nodo -> encoder.matches(token.trim(), nodo.getTokenHash()))
                .orElse(false);
        if (!valido) {
            log.warn("Token rechazado para el nodo {}", idNodo);
        }
        return valido;
    }

    String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
