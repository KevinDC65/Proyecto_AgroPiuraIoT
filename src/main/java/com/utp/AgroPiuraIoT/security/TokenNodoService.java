package com.utp.AgroPiuraIoT.security;

import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generación y verificación de tokens de autenticación para nodos IoT (A1/A2).
 * <p>
 * Formato: {@code ap_<idDispositivo>_<secretoBase64Url>}. El prefijo numérico
 * permite localizar el nodo en O(1) sin recorrer toda la tabla; el secreto se
 * verifica contra el hash BCrypt almacenado en {@code dispositivo_iot.token_hash}
 * y nunca se guarda ni registra en texto plano.
 */
@Service
public class TokenNodoService {

    private static final String PREFIJO = "ap_";
    private static final int BYTES_SECRETO = 32;

    private final DispositivoIoTRepository dispositivos;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public TokenNodoService(DispositivoIoTRepository dispositivos, PasswordEncoder passwordEncoder) {
        this.dispositivos = dispositivos;
        this.passwordEncoder = passwordEncoder;
    }

    /** Genera un token aleatorio seguro vinculado al nodo e informa su hash BCrypt. */
    public TokenGenerado generarPara(Integer idDispositivo) {
        if (idDispositivo == null || idDispositivo <= 0) {
            throw new IllegalArgumentException("El identificador del nodo es obligatorio");
        }
        byte[] bytes = new byte[BYTES_SECRETO];
        random.nextBytes(bytes);
        String secreto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenPlano = PREFIJO + idDispositivo + "_" + secreto;
        String hash = passwordEncoder.encode(tokenPlano);
        return new TokenGenerado(tokenPlano, hash);
    }

    /** Valida un token Bearer contra la base de datos. */
    @Transactional(readOnly = true)
    public ResultadoValidacion validar(String tokenPlano) {
        Optional<Integer> idOpt = extraerIdDispositivo(tokenPlano);
        if (idOpt.isEmpty()) {
            return ResultadoValidacion.invalido();
        }
        Optional<DispositivoIoT> dispOpt = dispositivos.findById(idOpt.get());
        if (dispOpt.isEmpty()) {
            return ResultadoValidacion.invalido();
        }
        DispositivoIoT dispositivo = dispOpt.get();
        if (!dispositivo.tieneToken() || !passwordEncoder.matches(tokenPlano, dispositivo.getTokenHash())) {
            return ResultadoValidacion.invalido();
        }
        if (!dispositivo.estaActivo()) {
            return ResultadoValidacion.inactivo(dispositivo.getIdDispositivo());
        }
        return ResultadoValidacion.valido(dispositivo.getIdDispositivo());
    }

    static Optional<Integer> extraerIdDispositivo(String tokenPlano) {
        if (tokenPlano == null || !tokenPlano.startsWith(PREFIJO)) {
            return Optional.empty();
        }
        String resto = tokenPlano.substring(PREFIJO.length());
        int separador = resto.indexOf('_');
        if (separador <= 0 || separador >= resto.length() - 1) {
            return Optional.empty();
        }
        try {
            int id = Integer.parseInt(resto.substring(0, separador));
            return id > 0 ? Optional.of(id) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record TokenGenerado(String tokenPlano, String tokenHash) {
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
