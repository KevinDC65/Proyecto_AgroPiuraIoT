package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.service.AlertasService;
import com.utp.AgroPiuraIoT.service.RecomendacionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Alertas de los cultivos del usuario y sus notificaciones (RF-05, B2). */
@Controller
public class AlertaController {

    private final AlertasService alertas;
    private final RecomendacionService recomendaciones;

    public AlertaController(AlertasService alertas, RecomendacionService recomendaciones) {
        this.alertas = alertas;
        this.recomendaciones = recomendaciones;
    }

    @GetMapping("/alertas")
    public String lista(Authentication auth, Model model) {
        List<Alerta> lista = alertas.listarPara(auth.getName());
        List<NotificacionUsuario> notificaciones = alertas.notificacionesDe(auth.getName());

        Map<Integer, String> recomendacionesPorAlerta = new LinkedHashMap<>();
        for (Alerta alerta : lista) {
            recomendacionesPorAlerta.put(alerta.getIdAlerta(), recomendaciones.para(alerta));
        }
        for (NotificacionUsuario notificacion : notificaciones) {
            Alerta alerta = notificacion.getAlerta();
            recomendacionesPorAlerta.computeIfAbsent(alerta.getIdAlerta(),
                    id -> recomendaciones.para(alerta));
        }

        model.addAttribute("alertas", lista);
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("recomendaciones", recomendacionesPorAlerta);
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
    public String marcarAtendida(@PathVariable Integer idAlerta,
                                 @RequestParam(required = false) String comentario,
                                 Authentication auth, RedirectAttributes flash) {
        alertas.marcarAtendida(idAlerta, auth.getName(), comentario);
        flash.addFlashAttribute("mensaje", "Alerta marcada como atendida.");
        return "redirect:/alertas";
    }
}
