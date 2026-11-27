package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Pattern LETRAS_Y_NUMEROS = Pattern.compile("^(?=.*\\p{L})(?=.*\\d).+$");
    private static final int LONGITUD_MINIMA_CLAVE = 8;
    private static final int LONGITUD_MAXIMA_CLAVE = 72;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de registro son obligatorios");
        }
        if (StringUtils.isBlank(request.nombre())) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (StringUtils.isBlank(request.correo())) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (StringUtils.isBlank(request.contrasena())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        validarPoliticaContrasena(request.contrasena());
        if (!request.clavesCoinciden()) {
            throw new ReglaNegocioException("Las contraseñas no coinciden");
        }
        String correo = StringUtils.lowerCase(StringUtils.trim(request.correo()));
        if (usuarios.existsByCorreo(correo)) {
            throw new ReglaNegocioException("Ese correo ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(StringUtils.trimToNull(request.nombre()));
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        return usuarios.save(usuario);
    }

    /**
     * Valida la política de contraseñas (C1): entre 8 y 72 caracteres con al menos
     * una letra y un número. Reutilizable en registro y cambio de contraseña.
     */
    public static void validarPoliticaContrasena(String contrasena) {
        if (contrasena == null
                || contrasena.length() < LONGITUD_MINIMA_CLAVE
                || contrasena.length() > LONGITUD_MAXIMA_CLAVE
                || !LETRAS_Y_NUMEROS.matcher(contrasena).matches()) {
            throw new ReglaNegocioException(
                    "La contraseña debe tener entre 8 y 72 caracteres e incluir letras y números");
        }
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {
        if (StringUtils.isBlank(correo)) {
            return null;
        }
        return usuarios.findByCorreo(StringUtils.lowerCase(StringUtils.trim(correo)))
                .orElse(null);
    }
}
