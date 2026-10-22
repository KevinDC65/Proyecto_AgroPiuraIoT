package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoSensorRepository extends JpaRepository<TipoSensor, Integer> {
}
