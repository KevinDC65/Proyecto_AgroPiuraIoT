package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.service.AlertasService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AlertaController {

    private final AlertasService alertas;
    private final UsuarioService usuarios;

    public AlertaController(AlertasService alertas, UsuarioService usuarios) {
        this.alertas = alertas;
        this.usuarios = usuarios;
    }

    @GetMapping("/alertas")
    String lista(Principal principal, Model model) {
        model.addAttribute("alertas", alertas.listarTodas());
        if (principal != null) {
            model.addAttribute("notificaciones", alertas.notificacionesDe(principal.getName()));
        }
        return "alertas/lista";
    }

    @PostMapping("/alertas/{idNotificacion}/leida")
    String marcarLeida(@PathVariable Integer idNotificacion, Principal principal) {
        if (principal != null) {
            alertas.marcarLeida(idNotificacion, principal.getName());
        }
        return "redirect:/alertas";
    }

    @PostMapping("/alertas/{idAlerta}/atender")
    String marcarAtendida(@PathVariable Integer idAlerta, RedirectAttributes redirect) {
        alertas.marcarAtendida(idAlerta);
        redirect.addFlashAttribute("mensaje", "Alerta marcada como atendida.");
        return "redirect:/alertas";
    }
}
