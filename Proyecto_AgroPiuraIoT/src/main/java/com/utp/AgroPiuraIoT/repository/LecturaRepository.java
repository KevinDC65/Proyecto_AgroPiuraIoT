package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Lectura;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturaRepository extends JpaRepository<Lectura, Integer> {

    List<Lectura> findBySensorIdSensorOrderByFechaHoraDesc(Integer idSensor);
}
