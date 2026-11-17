package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispositivoIoTRepository extends JpaRepository<DispositivoIoT, Integer> {

    /**
     * Borra los nodos asignados únicamente a este cultivo. Sus sensores, lecturas
     * y alertas se eliminan en la BD por ON DELETE CASCADE. Los nodos que también
     * están en otro cultivo no se tocan.
     *
     * @return cantidad de nodos eliminados
     */
    @Modifying
    @Query(value = """
            DELETE FROM dispositivo_iot d
            WHERE d.id_dispositivo IN (
                SELECT cd.id_dispositivo FROM cultivo_dispositivo cd
                WHERE cd.id_cultivo = :idCultivo
                  AND NOT EXISTS (
                      SELECT 1 FROM cultivo_dispositivo otro
                      WHERE otro.id_dispositivo = cd.id_dispositivo
                        AND otro.id_cultivo <> :idCultivo))
            """, nativeQuery = true)
    int eliminarExclusivosDeCultivo(@Param("idCultivo") Integer idCultivo);
}
