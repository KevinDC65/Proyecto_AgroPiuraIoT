package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Alerta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    List<Alerta> findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(Integer idCultivo);

    List<Alerta> findByUmbralIdUmbralAndEstado(Integer idUmbral, String estado);

    long countByUmbralCultivoIdCultivoAndAtendidaFalse(Integer idCultivo);

    long countByUmbralCultivoIdCultivoAndEstado(Integer idCultivo, String estado);

    /** Alertas de los cultivos a los que el usuario tiene acceso, más recientes primero. */
    @Query("""
            SELECT a FROM Alerta a
            WHERE a.umbral.cultivo.idCultivo IN (
                SELECT uc.cultivo.idCultivo FROM UsuarioCultivo uc WHERE uc.usuario.correo = :correo
            )
            ORDER BY a.fechaAlerta DESC
            """)
    List<Alerta> buscarPorUsuario(@Param("correo") String correo);

    /** Episodios de alerta abiertos en los cultivos del usuario. */
    @Query("""
            SELECT COUNT(a) FROM Alerta a
            WHERE a.estado = 'abierta'
            AND a.umbral.cultivo.idCultivo IN (
                SELECT uc.cultivo.idCultivo FROM UsuarioCultivo uc WHERE uc.usuario.correo = :correo
            )
            """)
    long contarAbiertasPorUsuario(@Param("correo") String correo);

    /** Alias histórico mantenido por compatibilidad: cuenta episodios abiertos del usuario. */
    default long contarActivasPorUsuario(String correo) {
        return contarAbiertasPorUsuario(correo);
    }
}
