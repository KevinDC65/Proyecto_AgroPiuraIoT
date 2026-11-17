package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de accesos por cultivo (RF-07): un administrador asigna o quita
 * el acceso de un usuario a un cultivo y le define el rol
 * (administrador o colaborador). Validamos los datos en vez de fallar en
 * silencio y nunca dejamos un cultivo sin administrador.
 */
@Service
public class AccesoService {

    private static final Logger log = LoggerFactory.getLogger(AccesoService.class);

    public static final String ROL_COLABORADOR = "colaborador";
    public static final String ROL_ADMINISTRADOR = "administrador";

    private final UsuarioCultivoRepository accesos;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final CultivoRepository cultivos;

    public AccesoService(UsuarioCultivoRepository accesos, UsuarioRepository usuarios,
                         RolRepository roles, CultivoRepository cultivos) {
        this.accesos = accesos;
        this.usuarios = usuarios;
        this.roles = roles;
        this.cultivos = cultivos;
    }

    /** Usuarios con acceso al cultivo y su rol. */
    @Transactional(readOnly = true)
    public List<UsuarioCultivo> listar(Integer idCultivo) {
        return accesos.findByCultivoIdCultivo(idCultivo);
    }

    /**
     * Asigna el rol de un usuario en un cultivo; si ya tenía acceso,
     * actualiza el rol en lugar de duplicar la fila.
     */
    @Transactional
    public UsuarioCultivo asignar(Integer idCultivo, String correo, String nombreRol) {
        if (StringUtils.isBlank(correo)) {
            throw new ReglaNegocioException("El correo es obligatorio");
        }
        if (!ROL_ADMINISTRADOR.equals(nombreRol) && !ROL_COLABORADOR.equals(nombreRol)) {
            throw new ReglaNegocioException("Rol no reconocido: " + nombreRol);
        }
        Cultivo cultivo = cultivos.findById(idCultivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cultivo", idCultivo));
        Usuario usuario = buscarUsuario(correo);
        Rol rol = roles.findByNombreRolIgnoreCase(nombreRol)
                .orElseThrow(() -> new IllegalStateException("Rol no sembrado: " + nombreRol));

        Optional<UsuarioCultivo> existente =
                accesos.findByCultivoIdCultivoAndUsuarioIdUsuario(idCultivo, usuario.getIdUsuario());
        if (existente.isPresent() && esAdministrador(existente.get()) && !rol.esAdministrador()) {
            exigirOtroAdministrador(idCultivo, "No puedes degradar al único administrador del cultivo");
        }
        UsuarioCultivo acceso = existente.orElseGet(() -> nuevoAcceso(usuario, cultivo));
        acceso.setRol(rol);
        UsuarioCultivo guardado = accesos.save(acceso);
        log.info("Acceso al cultivo {} asignado a {} como {}", idCultivo, usuario.getCorreo(), nombreRol);
        return guardado;
    }

    /** Quita el acceso de un usuario a un cultivo (nunca al último administrador). */
    @Transactional
    public void quitar(Integer idCultivo, String correo) {
        Usuario usuario = buscarUsuario(correo);
        Optional<UsuarioCultivo> acceso =
                accesos.findByCultivoIdCultivoAndUsuarioIdUsuario(idCultivo, usuario.getIdUsuario());
        if (acceso.isEmpty()) {
            return; // idempotente: ya no tenía acceso
        }
        if (esAdministrador(acceso.get())) {
            exigirOtroAdministrador(idCultivo, "No puedes quitar al único administrador del cultivo");
        }
        accesos.delete(acceso.get());
        log.info("Acceso al cultivo {} retirado a {}", idCultivo, usuario.getCorreo());
    }

    private Usuario buscarUsuario(String correo) {
        String correoNormalizado = StringUtils.lowerCase(StringUtils.trim(correo));
        return usuarios.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new ReglaNegocioException("No existe un usuario con ese correo"));
    }

    private void exigirOtroAdministrador(Integer idCultivo, String mensaje) {
        if (accesos.countByCultivoIdCultivoAndRolNombreRolIgnoreCase(idCultivo, ROL_ADMINISTRADOR) <= 1) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    private static boolean esAdministrador(UsuarioCultivo acceso) {
        return acceso.getRol() != null && acceso.getRol().esAdministrador();
    }

    private static UsuarioCultivo nuevoAcceso(Usuario usuario, Cultivo cultivo) {
        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setUsuario(usuario);
        acceso.setCultivo(cultivo);
        return acceso;
    }
}
