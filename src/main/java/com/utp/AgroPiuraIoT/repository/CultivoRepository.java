package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CultivoRepository extends JpaRepository<Cultivo, Integer> {

    List<Cultivo> findByNombreContainingIgnoreCase(String nombre);

    Optional<Cultivo> findByNombre(String nombre);

    List<Cultivo> findByDispositivosIdDispositivo(Integer idDispositivo);
}
