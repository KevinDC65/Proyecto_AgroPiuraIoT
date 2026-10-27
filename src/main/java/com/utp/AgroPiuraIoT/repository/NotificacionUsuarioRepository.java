package com.utp.AgroPiuraIoT.repository;

import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionUsuarioRepository extends JpaRepository<NotificacionUsuario, Integer> {

    List<NotificacionUsuario> findByUsuarioIdUsuarioAndLeidoFalse(Integer idUsuario);

    List<NotificacionUsuario> findByAlertaIdAlerta(Integer idAlerta);

    List<NotificacionUsuario> findByUsuarioCorreoAndLeidoFalseOrderByIdNotificacionDesc(String correo);

    long countByUsuarioCorreoAndLeidoFalse(String correo);
}
