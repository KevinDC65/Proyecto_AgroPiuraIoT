package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.PlantillaUmbral;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaUmbralRepository extends JpaRepository<PlantillaUmbral, Integer> {

    List<PlantillaUmbral> findByTipoCultivoOrderByTipoSensorNombreTipo(String tipoCultivo);

    Optional<PlantillaUmbral> findByTipoCultivoAndTipoSensor_IdTipoSensor(
            String tipoCultivo, Integer idTipoSensor);
}
