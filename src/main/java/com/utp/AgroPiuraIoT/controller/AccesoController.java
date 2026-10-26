package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.service.AccesoService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestión de accesos por cultivo (RF-07): el administrador del sistema asigna
 * o quita usuarios del cultivo definiendo su rol. Solo las acciones de
 * escritura (POST) están restringidas a ROLE_ADMIN en SecurityConfig.
 */
@Controller
public class AccesoController {

    private final AccesoService accesos;
    private final CultivoService cultivos;

    public AccesoController(AccesoService accesos, CultivoService cultivos) {
        this.accesos = accesos;
        this.cultivos = cultivos;
    }

    @GetMapping("/cultivos/{idCultivo}/accesos")
    String lista(@PathVariable Integer idCultivo, Model model) {
        model.addAttribute("cultivo", cultivos.buscar(idCultivo));
        model.addAttribute("accesos", accesos.listar(idCultivo));
        return "accesos/lista";
    }

    @PostMapping("/cultivos/{idCultivo}/accesos")
    String asignar(@PathVariable Integer idCultivo,
                   @RequestParam String correo,
                   @RequestParam String rol,
                   RedirectAttributes flash) {
        try {
            accesos.asignar(idCultivo, correo, rol);
            flash.addFlashAttribute("mensaje", "Acceso asignado correctamente");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cultivos/" + idCultivo + "/accesos";
    }

    @PostMapping("/cultivos/{idCultivo}/accesos/quitar")
    String quitar(@PathVariable Integer idCultivo,
                  @RequestParam String correo,
                  RedirectAttributes flash) {
        try {
            accesos.quitar(idCultivo, correo);
            flash.addFlashAttribute("mensaje", "Acceso retirado");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cultivos/" + idCultivo + "/accesos";
    }
}
