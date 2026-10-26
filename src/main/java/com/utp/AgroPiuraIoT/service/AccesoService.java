package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de accesos por cultivo (RF-07): un administrador asigna o quita
 * el acceso de un usuario a un cultivo y le define el rol
 * (administrador o colaborador). Validamos los datos en vez de fallar en silencio.
 */
@Service
public class AccesoService {

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
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (!ROL_ADMINISTRADOR.equals(nombreRol) && !ROL_COLABORADOR.equals(nombreRol)) {
            throw new IllegalArgumentException("Rol no reconocido: " + nombreRol);
        }
        Cultivo cultivo = cultivos.findById(idCultivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cultivo no encontrado: " + idCultivo));
        String correoNormalizado = StringUtils.lowerCase(StringUtils.trim(correo));
        Usuario usuario = usuarios.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un usuario con ese correo"));
        Rol rol = roles.findByNombreRolIgnoreCase(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no registrado: " + nombreRol));

        UsuarioCultivo acceso = accesos.findByCultivoIdCultivo(idCultivo).stream()
                .filter(a -> a.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
                .findFirst()
                .orElseGet(() -> nuevoAcceso(usuario, cultivo, rol));
        acceso.setRol(rol);
        return accesos.save(acceso);
    }

    /** Quita el acceso de un usuario a un cultivo. */
    @Transactional
    public void quitar(Integer idCultivo, String correo) {
        String correoNormalizado = StringUtils.lowerCase(StringUtils.trim(correo));
        Usuario usuario = usuarios.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un usuario con ese correo"));
        accesos.findByCultivoIdCultivo(idCultivo).stream()
                .filter(a -> a.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
                .findFirst()
                .ifPresent(accesos::delete);
    }

    private UsuarioCultivo nuevoAcceso(Usuario usuario, Cultivo cultivo, Rol rol) {
        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setUsuario(usuario);
        acceso.setCultivo(cultivo);
        acceso.setRol(rol);
        return acceso;
    }
}
