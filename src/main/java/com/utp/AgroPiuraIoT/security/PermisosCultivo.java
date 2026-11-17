package com.utp.AgroPiuraIoT.security;

import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorización por recurso (RF-07). El rol de un usuario no es global: se
 * define por cultivo en {@code usuario_cultivo}. Se usa desde
 * {@code @PreAuthorize("@permisos.puedeVer(#idCultivo, authentication)")}.
 * <ul>
 *   <li>Ver y operar (consultar, simular, atender alertas): cualquier miembro del cultivo.</li>
 *   <li>Administrar (editar, eliminar, dispositivos, umbrales, accesos): solo su administrador.</li>
 * </ul>
 * Un cultivo inexistente se trata igual que uno ajeno (403) para no revelar qué ids existen.
 */
@Component("permisos")
public class PermisosCultivo {

    private final UsuarioCultivoRepository accesos;
    private final AlertaRepository alertas;

    public PermisosCultivo(UsuarioCultivoRepository accesos, AlertaRepository alertas) {
        this.accesos = accesos;
        this.alertas = alertas;
    }

    @Transactional(readOnly = true)
    public boolean puedeVer(Integer idCultivo, Authentication auth) {
        return acceso(idCultivo, auth).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean puedeAdministrar(Integer idCultivo, Authentication auth) {
        return acceso(idCultivo, auth)
                .map(a -> a.getRol() != null && a.getRol().esAdministrador())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean puedeVerAlerta(Integer idAlerta, Authentication auth) {
        if (idAlerta == null) {
            return false;
        }
        return alertas.findById(idAlerta)
                .map(a -> puedeVer(a.getUmbral().getCultivo().getIdCultivo(), auth))
                .orElse(false);
    }

    private Optional<UsuarioCultivo> acceso(Integer idCultivo, Authentication auth) {
        if (idCultivo == null || !autenticado(auth)) {
            return Optional.empty();
        }
        return accesos.findByCultivoIdCultivoAndUsuarioCorreo(idCultivo, auth.getName());
    }

    private static boolean autenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }
}
