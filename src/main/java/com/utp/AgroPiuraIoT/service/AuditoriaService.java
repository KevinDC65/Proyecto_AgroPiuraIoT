package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.Auditoria;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.repository.AuditoriaRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio transversal de auditoría (C1): registra acciones críticas de negocio y
 * autenticación en la tabla {@code auditoria} para trazabilidad de los frentes A, B, C y D.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    public static final String ACCION_LOGIN_EXITOSO = "LOGIN_EXITOSO";
    public static final String ACCION_LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String ACCION_CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA";

    public static final String ENTIDAD_USUARIO = "usuario";

    private static final int MAX_ACCION = 80;
    private static final int MAX_ENTIDAD = 80;
    private static final int MAX_DETALLE = 500;

    private final AuditoriaRepository auditorias;
    private final UsuarioRepository usuarios;

    public AuditoriaService(AuditoriaRepository auditorias, UsuarioRepository usuarios) {
        this.auditorias = auditorias;
        this.usuarios = usuarios;
    }

    /**
     * Registra un evento de auditoría. Nunca incluir contraseñas ni tokens en {@code detalle}.
     *
     * @param usuario   usuario que ejecuta la acción (puede ser {@code null} en intentos fallidos
     *                  con correos no registrados)
     * @param accion    código de acción en mayúsculas (ej. {@code LOGIN_EXITOSO}, {@code UMBRAL_ACTUALIZADO})
     * @param entidad   nombre de la entidad afectada en minúsculas (ej. {@code usuario}, {@code cultivo})
     * @param idEntidad identificador de la entidad afectada (opcional)
     * @param detalle   descripción legible y sin datos sensibles (máx. 500 caracteres)
     */
    @Transactional
    public Auditoria registrar(Usuario usuario, String accion, String entidad, Integer idEntidad, String detalle) {
        if (StringUtils.isBlank(accion)) {
            throw new IllegalArgumentException("La acción de auditoría es obligatoria");
        }
        if (StringUtils.isBlank(entidad)) {
            throw new IllegalArgumentException("La entidad de auditoría es obligatoria");
        }
        Auditoria registro = new Auditoria();
        registro.setUsuario(usuario);
        registro.setAccion(recortar(accion.trim().toUpperCase(), MAX_ACCION));
        registro.setEntidad(recortar(entidad.trim().toLowerCase(), MAX_ENTIDAD));
        registro.setIdEntidad(idEntidad);
        registro.setDetalle(recortar(StringUtils.trimToNull(detalle), MAX_DETALLE));

        Auditoria guardado = auditorias.save(registro);
        log.debug("Auditoría {}: accion={}, entidad={}, idEntidad={}, idUsuario={}",
                guardado.getId(), guardado.getAccion(), guardado.getEntidad(), guardado.getIdEntidad(),
                usuario != null ? usuario.getIdUsuario() : null);
        return guardado;
    }

    /**
     * Atajo para servicios o controladores que disponen del correo del usuario autenticado.
     */
    @Transactional
    public Auditoria registrarPorCorreo(String correo, String accion, String entidad, Integer idEntidad, String detalle) {
        Usuario usuario = StringUtils.isBlank(correo)
                ? null
                : usuarios.findByCorreo(correo.trim().toLowerCase()).orElse(null);
        return registrar(usuario, accion, entidad, idEntidad, detalle);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listarPorEntidad(String entidad, Integer idEntidad) {
        return auditorias.findByEntidadAndIdEntidadOrderByFechaDesc(entidad, idEntidad);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listarPorUsuario(Integer idUsuario) {
        return auditorias.findByUsuarioIdUsuarioOrderByFechaDesc(idUsuario);
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo);
    }
}
