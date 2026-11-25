package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Punto único para dejar rastro de acciones sensibles.
 * <p>
 * Por ahora escribe una línea estructurada en el log: la tabla persistente de
 * auditoría pertenece al issue C1 y aún no existe en el esquema. Cuando C1 la
 * implemente, solo cambia esta clase; los llamadores no se tocan.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    public void registrar(Usuario usuario, String accion, String entidad,
                          Integer idEntidad, String detalle) {
        log.info("AUDITORIA accion={} usuario={} entidad={} id={} detalle={}",
                accion,
                usuario == null ? "sistema" : usuario.getCorreo(),
                entidad,
                idEntidad,
                detalle == null ? "" : detalle);
    }
}
