package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioCultivoRepository extends JpaRepository<UsuarioCultivo, Integer> {

    List<UsuarioCultivo> findByUsuarioIdUsuario(Integer idUsuario);

    List<UsuarioCultivo> findByCultivoIdCultivo(Integer idCultivo);

    List<UsuarioCultivo> findByUsuarioCorreo(String correo);

    Optional<UsuarioCultivo> findByCultivoIdCultivoAndUsuarioCorreo(Integer idCultivo, String correo);

    Optional<UsuarioCultivo> findByCultivoIdCultivoAndUsuarioIdUsuario(Integer idCultivo, Integer idUsuario);

    long countByCultivoIdCultivoAndRolNombreRolIgnoreCase(Integer idCultivo, String nombreRol);
}
