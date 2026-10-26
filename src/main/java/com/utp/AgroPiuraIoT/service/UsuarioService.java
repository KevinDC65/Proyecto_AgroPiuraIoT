package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (!request.clavesCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        String correo = StringUtils.lowerCase(StringUtils.trim(request.correo()));
        if (usuarios.existsByCorreo(correo)) {
            throw new IllegalArgumentException("Ese correo ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(StringUtils.trimToNull(request.nombre()));
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        return usuarios.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {
        return usuarios.findByCorreo(StringUtils.lowerCase(StringUtils.trim(correo)))
                .orElse(null);
    }
}
