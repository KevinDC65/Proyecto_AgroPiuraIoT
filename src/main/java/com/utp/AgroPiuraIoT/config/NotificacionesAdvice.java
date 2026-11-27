package com.utp.AgroPiuraIoT.config;

import com.utp.AgroPiuraIoT.service.AlertasService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class NotificacionesAdvice {

    private final AlertasService alertas;

    public NotificacionesAdvice(AlertasService alertas) {
        this.alertas = alertas;
    }

    /** Expone el número de episodios de alerta abiertos para el contador del menú lateral. */
    @ModelAttribute("notificacionesPendientes")
    public long notificacionesPendientes(Principal principal, HttpServletRequest request) {
        if (principal == null || request.getRequestURI().startsWith("/api/")) {
            return 0;
        }
        return alertas.contarAbiertasPorUsuario(principal.getName());
    }
}