package com.utp.AgroPiuraIoT.security;

import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.service.AuditoriaService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escucha eventos de autenticación de Spring Security (C1) para:
 * <ul>
 *   <li>Contar intentos fallidos y bloquear temporalmente la cuenta tras el umbral configurado.</li>
 *   <li>Reiniciar el contador al iniciar sesión correctamente o al expirar un bloqueo previo.</li>
 *   <li>Registrar inicios de sesión, fallos y bloqueos en {@link AuditoriaService}.</li>
 * </ul>
 */
@Component
public class EventosAutenticacionListener {

    private static final Logger log = LoggerFactory.getLogger(EventosAutenticacionListener.class);

    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final int maxIntentosFallidos;
    private final long bloqueoMinutos;

    public EventosAutenticacionListener(
            UsuarioRepository usuarios,
            AuditoriaService auditoria,
            @Value("${agro.seguridad.max-intentos-fallidos:5}") int maxIntentosFallidos,
            @Value("${agro.seguridad.bloqueo-minutos:15}") long bloqueoMinutos) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.maxIntentosFallidos = Math.max(1, maxIntentosFallidos);
        this.bloqueoMinutos = Math.max(1, bloqueoMinutos);
    }

    @EventListener
    @Transactional
    public void alIniciarSesionConExito(AuthenticationSuccessEvent event) {
        String correo = normalizarCorreo(event.getAuthentication() != null
                ? event.getAuthentication().getName()
                : null);
        if (correo == null) {
            return;
        }
        Optional<Usuario> encontrado = usuarios.findByCorreo(correo);
        if (encontrado.isEmpty()) {
            return;
        }
        Usuario usuario = encontrado.get();
        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuario.reiniciarIntentosFallidos();
            usuarios.save(usuario);
        }
        auditoria.registrar(
                usuario,
                AuditoriaService.ACCION_LOGIN_EXITOSO,
                AuditoriaService.ENTIDAD_USUARIO,
                usuario.getIdUsuario(),
                "Inicio de sesión exitoso");
    }

    @EventListener
    @Transactional
    public void alFallarCredenciales(AuthenticationFailureBadCredentialsEvent event) {
        String correo = normalizarCorreo(event.getAuthentication() != null
                ? event.getAuthentication().getName()
                : null);
        if (correo == null) {
            return;
        }
        Optional<Usuario> encontrado = usuarios.findByCorreo(correo);
        if (encontrado.isEmpty()) {
            auditoria.registrar(
                    null,
                    AuditoriaService.ACCION_LOGIN_FALLIDO,
                    AuditoriaService.ENTIDAD_USUARIO,
                    null,
                    "Intento de inicio de sesión con correo no registrado");
            return;
        }

        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now();

        // Si un bloqueo previo ya venció, el contador arranca limpio para el nuevo ciclo.
        if (usuario.getBloqueadoHasta() != null && !usuario.estaBloqueado(ahora)) {
            usuario.reiniciarIntentosFallidos();
        }

        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);

        if (intentos >= maxIntentosFallidos) {
            usuario.setBloqueadoHasta(ahora.plusMinutes(bloqueoMinutos));
            usuarios.save(usuario);
            auditoria.registrar(
                    usuario,
                    AuditoriaService.ACCION_CUENTA_BLOQUEADA,
                    AuditoriaService.ENTIDAD_USUARIO,
                    usuario.getIdUsuario(),
                    "Cuenta bloqueada por " + bloqueoMinutos + " minutos tras "
                            + intentos + " intentos fallidos");
            log.warn("Cuenta {} bloqueada por {} minutos tras {} intentos fallidos",
                    usuario.getIdUsuario(), bloqueoMinutos, intentos);
        } else {
            usuarios.save(usuario);
            auditoria.registrar(
                    usuario,
                    AuditoriaService.ACCION_LOGIN_FALLIDO,
                    AuditoriaService.ENTIDAD_USUARIO,
                    usuario.getIdUsuario(),
                    "Credenciales inválidas (intento " + intentos + " de " + maxIntentosFallidos + ")");
        }
    }

    @EventListener
    @Transactional
    public void alIntentarCuentaBloqueada(AuthenticationFailureLockedEvent event) {
        String correo = normalizarCorreo(event.getAuthentication() != null
                ? event.getAuthentication().getName()
                : null);
        if (correo == null) {
            return;
        }
        usuarios.findByCorreo(correo).ifPresent(usuario -> auditoria.registrar(
                usuario,
                AuditoriaService.ACCION_LOGIN_FALLIDO,
                AuditoriaService.ENTIDAD_USUARIO,
                usuario.getIdUsuario(),
                "Intento de inicio de sesión sobre cuenta bloqueada temporalmente"));
    }

    @EventListener
    @Transactional
    public void alIntentarCuentaInactiva(AuthenticationFailureDisabledEvent event) {
        String correo = normalizarCorreo(event.getAuthentication() != null
                ? event.getAuthentication().getName()
                : null);
        if (correo == null) {
            return;
        }
        usuarios.findByCorreo(correo).ifPresent(usuario -> auditoria.registrar(
                usuario,
                AuditoriaService.ACCION_LOGIN_FALLIDO,
                AuditoriaService.ENTIDAD_USUARIO,
                usuario.getIdUsuario(),
                "Intento de inicio de sesión sobre cuenta inactiva"));
    }

    private static String normalizarCorreo(String correo) {
        if (StringUtils.isBlank(correo)) {
            return null;
        }
        return correo.trim().toLowerCase();
    }
}
