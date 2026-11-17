package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.service.AlertasService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Alertas de los cultivos del usuario y sus notificaciones (RF-05). */
@Controller
public class AlertaController {

    private final AlertasService alertas;

    public AlertaController(AlertasService alertas) {
        this.alertas = alertas;
    }

    @GetMapping("/alertas")
    public String lista(Authentication auth, Model model) {
        model.addAttribute("alertas", alertas.listarPara(auth.getName()));
        model.addAttribute("notificaciones", alertas.notificacionesDe(auth.getName()));
        return "alertas/lista";
    }

    /** La propiedad de la notificación se verifica en el servicio (ajena = 404). */
    @PostMapping("/alertas/{idNotificacion:\\d+}/leida")
    public String marcarLeida(@PathVariable Integer idNotificacion, Authentication auth) {
        alertas.marcarLeida(idNotificacion, auth.getName());
        return "redirect:/alertas";
    }

    @PostMapping("/alertas/{idAlerta:\\d+}/atender")
    @PreAuthorize("@permisos.puedeVerAlerta(#idAlerta, authentication)")
    public String marcarAtendida(@PathVariable Integer idAlerta, RedirectAttributes flash) {
        alertas.marcarAtendida(idAlerta);
        flash.addFlashAttribute("mensaje", "Alerta marcada como atendida.");
        return "redirect:/alertas";
    }
}
