package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Sensor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {

    List<Sensor> findByDispositivoIdDispositivo(Integer idDispositivo);
}
