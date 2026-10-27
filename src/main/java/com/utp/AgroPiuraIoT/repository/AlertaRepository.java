package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Alerta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    List<Alerta> findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(Integer idCultivo);

    List<Alerta> findAllByOrderByFechaAlertaDesc();

    long countByAtendidaFalse();

    long countByUmbralCultivoIdCultivoAndAtendidaFalse(Integer idCultivo);
}
