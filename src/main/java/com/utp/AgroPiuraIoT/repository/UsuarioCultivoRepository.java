package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioCultivoRepository extends JpaRepository<UsuarioCultivo, Integer> {

    List<UsuarioCultivo> findByUsuarioIdUsuario(Integer idUsuario);

    List<UsuarioCultivo> findByCultivoIdCultivo(Integer idCultivo);
}
