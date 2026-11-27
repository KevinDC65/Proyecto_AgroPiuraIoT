package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.AlertaNodo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaNodoRepository extends JpaRepository<AlertaNodo, Integer> {

    List<AlertaNodo> findByDispositivoIdDispositivoAndEstado(Integer idDispositivo, String estado);

    boolean existsByDispositivoIdDispositivoAndCultivoIdCultivoAndEstado(
            Integer idDispositivo, Integer idCultivo, String estado);

    @Query("""
            SELECT a FROM AlertaNodo a
            WHERE a.cultivo.idCultivo IN (
                SELECT uc.cultivo.idCultivo FROM UsuarioCultivo uc WHERE uc.usuario.correo = :correo
            )
            ORDER BY a.fechaAlerta DESC
            """)
    List<AlertaNodo> buscarPorUsuario(@Param("correo") String correo);

    @Query("""
            SELECT COUNT(a) FROM AlertaNodo a
            WHERE a.estado = 'abierta'
            AND a.cultivo.idCultivo IN (
                SELECT uc.cultivo.idCultivo FROM UsuarioCultivo uc WHERE uc.usuario.correo = :correo
            )
            """)
    long contarAbiertasPorUsuario(@Param("correo") String correo);
}
