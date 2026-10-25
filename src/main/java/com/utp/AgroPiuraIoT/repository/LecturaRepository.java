package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Lectura;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LecturaRepository extends JpaRepository<Lectura, Integer> {

    List<Lectura> findBySensorIdSensorOrderByFechaHoraDesc(Integer idSensor);

    /**
     * Lecturas del cultivo indicado (el dispositivo del sensor debe estar
     * asignado a ese cultivo). Ordenadas de la más reciente a la más antigua.
     */
    @Query("""
            SELECT l FROM Lectura l
            WHERE l.sensor.dispositivo IN (
                SELECT d FROM Cultivo c JOIN c.dispositivos d WHERE c.idCultivo = :idCultivo
            )
            ORDER BY l.fechaHora DESC
            """)
    List<Lectura> buscarPorCultivo(@Param("idCultivo") Integer idCultivo);

    /** Igual que buscarPorCultivo, limitado al rango [desde, hasta] de fecha y hora. */
    @Query("""
            SELECT l FROM Lectura l
            WHERE l.sensor.dispositivo IN (
                SELECT d FROM Cultivo c JOIN c.dispositivos d WHERE c.idCultivo = :idCultivo
            )
            AND l.fechaHora >= :desde
            AND l.fechaHora <= :hasta
            ORDER BY l.fechaHora DESC
            """)
    List<Lectura> buscarPorCultivoYRango(@Param("idCultivo") Integer idCultivo,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);
}
