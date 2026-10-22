package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombreRolIgnoreCase(String nombreRol);
}
