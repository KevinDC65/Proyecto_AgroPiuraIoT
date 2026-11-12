package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Sensor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {

    List<Sensor> findByDispositivoIdDispositivo(Integer idDispositivo);

    Optional<Sensor> findByDispositivoIdDispositivoAndTipoSensorNombreTipoIgnoreCase(
            Integer idDispositivo, String nombreTipo);

    Optional<Sensor> findByDispositivoIdDispositivoAndTipoSensorIdTipoSensor(
            Integer idDispositivo, Integer idTipoSensor);
}
