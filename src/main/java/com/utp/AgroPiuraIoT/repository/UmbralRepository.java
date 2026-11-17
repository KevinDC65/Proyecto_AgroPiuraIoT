package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Umbral;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UmbralRepository extends JpaRepository<Umbral, Integer> {

    List<Umbral> findByCultivoIdCultivo(Integer idCultivo);

    Optional<Umbral> findByCultivoIdCultivoAndTipoSensorIdTipoSensor(Integer idCultivo, Integer idTipoSensor);
}
