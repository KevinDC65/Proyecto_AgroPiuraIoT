package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {

    List<Auditoria> findByUsuarioIdUsuarioOrderByFechaDesc(Integer idUsuario);

    List<Auditoria> findByEntidadAndIdEntidadOrderByFechaDesc(String entidad, Integer idEntidad);

    List<Auditoria> findByAccionOrderByFechaDesc(String accion);
}
