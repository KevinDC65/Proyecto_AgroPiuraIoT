package com.utp.AgroPiuraIoT.security;

import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final UsuarioCultivoRepository accesos;

    public AppUserDetailsService(UsuarioRepository usuarios, UsuarioCultivoRepository accesos) {
        this.usuarios = usuarios;
        this.accesos = accesos;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));
        Set<GrantedAuthority> autoridades = new HashSet<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (UsuarioCultivo acceso : accesos.findByUsuarioIdUsuario(usuario.getIdUsuario())) {
            if (acceso.getRol() != null && acceso.getRol().esAdministrador()) {
                autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                break;
            }
        }
        boolean activo = "activo".equalsIgnoreCase(usuario.getEstado());
        return new User(usuario.getCorreo(), usuario.getContrasena(), activo, true, true, true, autoridades);
    }
}
