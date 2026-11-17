package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CultivoRepository extends JpaRepository<Cultivo, Integer> {

    Optional<Cultivo> findByNombre(String nombre);

    List<Cultivo> findByDispositivosIdDispositivo(Integer idDispositivo);

    /** Cultivos a los que el usuario tiene acceso (cualquier rol), por nombre. */
    @Query("""
            SELECT uc.cultivo FROM UsuarioCultivo uc
            WHERE uc.usuario.correo = :correo
            ORDER BY uc.cultivo.nombre, uc.cultivo.idCultivo
            """)
    List<Cultivo> buscarPorUsuario(@Param("correo") String correo);

    /**
     * Cultivos sin ningún administrador: nadie los ve en la web. Suelen ser datos
     * creados antes de los permisos por cultivo (ver database/migracion-datos-anteriores.sql).
     */
    @Query("""
            SELECT c FROM Cultivo c
            WHERE NOT EXISTS (
                SELECT 1 FROM UsuarioCultivo uc
                WHERE uc.cultivo = c AND LOWER(uc.rol.nombreRol) = 'administrador')
            ORDER BY c.idCultivo
            """)
    List<Cultivo> findSinAdministrador();
}
