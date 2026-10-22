package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositivoIoTRepository extends JpaRepository<DispositivoIoT, Integer> {

    List<DispositivoIoT> findByEstadoIgnoreCase(String estado);
}
