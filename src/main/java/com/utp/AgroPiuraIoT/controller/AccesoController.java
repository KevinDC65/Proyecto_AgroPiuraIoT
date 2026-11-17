package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.security.PermisosCultivo;
import com.utp.AgroPiuraIoT.service.AccesoService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestión de accesos por cultivo (RF-07): cualquier miembro ve quién tiene
 * acceso; solo el administrador de ese cultivo asigna o quita usuarios.
 */
@Controller
public class AccesoController {

    private final AccesoService accesos;
    private final CultivoService cultivos;
    private final PermisosCultivo permisos;

    public AccesoController(AccesoService accesos, CultivoService cultivos, PermisosCultivo permisos) {
        this.accesos = accesos;
        this.cultivos = cultivos;
        this.permisos = permisos;
    }

    @GetMapping("/cultivos/{idCultivo:\\d+}/accesos")
    @PreAuthorize("@permisos.puedeVer(#idCultivo, authentication)")
    public String lista(@PathVariable Integer idCultivo, Authentication auth, Model model) {
        model.addAttribute("cultivo", cultivos.buscar(idCultivo));
        model.addAttribute("accesos", accesos.listar(idCultivo));
        model.addAttribute("puedeAdministrar", permisos.puedeAdministrar(idCultivo, auth));
        return "accesos/lista";
    }

    @PostMapping("/cultivos/{idCultivo:\\d+}/accesos")
    @PreAuthorize("@permisos.puedeAdministrar(#idCultivo, authentication)")
    public String asignar(@PathVariable Integer idCultivo,
                          @RequestParam(defaultValue = "") String correo,
                          @RequestParam(defaultValue = "") String rol,
                          RedirectAttributes flash) {
        try {
            accesos.asignar(idCultivo, correo, rol);
            flash.addFlashAttribute("mensaje", "Acceso asignado correctamente");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cultivos/" + idCultivo + "/accesos";
    }

    @PostMapping("/cultivos/{idCultivo:\\d+}/accesos/quitar")
    @PreAuthorize("@permisos.puedeAdministrar(#idCultivo, authentication)")
    public String quitar(@PathVariable Integer idCultivo,
                         @RequestParam(defaultValue = "") String correo,
                         RedirectAttributes flash) {
        try {
            accesos.quitar(idCultivo, correo);
            flash.addFlashAttribute("mensaje", "Acceso retirado");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cultivos/" + idCultivo + "/accesos";
    }
}
