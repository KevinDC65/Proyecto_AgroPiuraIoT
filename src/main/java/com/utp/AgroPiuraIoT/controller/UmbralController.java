package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.security.PermisosCultivo;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Umbrales por cultivo y variable (RF-03): los miembros del cultivo los
 * consultan; solo su administrador los crea, edita o elimina.
 */
@Controller
public class UmbralController {

    private final UmbralService umbrales;
    private final CultivoService cultivos;
    private final PermisosCultivo permisos;

    public UmbralController(UmbralService umbrales, CultivoService cultivos, PermisosCultivo permisos) {
        this.umbrales = umbrales;
        this.cultivos = cultivos;
        this.permisos = permisos;
    }

    /** Entrada del menú lateral: abre la configuración del primer cultivo del usuario. */
    @GetMapping("/umbrales")
    public String inicio(Authentication auth) {
        List<Cultivo> propios = cultivos.listarPara(auth.getName());
        return propios.isEmpty()
                ? "redirect:/cultivos"
                : "redirect:/cultivos/" + propios.get(0).getIdCultivo() + "/umbrales";
    }

    @GetMapping("/cultivos/{idCultivo:\\d+}/umbrales")
    @PreAuthorize("@permisos.puedeVer(#idCultivo, authentication)")
    public String lista(@PathVariable Integer idCultivo,
                        @RequestParam(required = false) Integer editar,
                        Authentication auth, Model model) {
        boolean puedeAdministrar = permisos.puedeAdministrar(idCultivo, auth);
        if (editar != null && puedeAdministrar) {
            Umbral umbral = umbrales.buscar(idCultivo, editar);
            model.addAttribute("umbralEditado", umbral);
            model.addAttribute("form", new UmbralForm(
                    umbral.getTipoSensor().getIdTipoSensor(),
                    umbral.getValorMinimo(),
                    umbral.getValorMaximo()));
        } else if (!model.containsAttribute("form")) {
            model.addAttribute("form", new UmbralForm(null, null, null));
        }
        prepararVista(idCultivo, puedeAdministrar, model);
        return "umbrales/lista";
    }

    @PostMapping("/cultivos/{idCultivo:\\d+}/umbrales")
    @PreAuthorize("@permisos.puedeAdministrar(#idCultivo, authentication)")
    public String guardar(@PathVariable Integer idCultivo,
                          @Valid @ModelAttribute("form") UmbralForm form,
                          BindingResult errores, Model model, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                umbrales.guardar(idCultivo, form);
                flash.addFlashAttribute("mensaje", "Umbral guardado");
                return "redirect:/cultivos/" + idCultivo + "/umbrales";
            } catch (ReglaNegocioException e) {
                errores.reject("umbral", e.getMessage());
            }
        }
        prepararVista(idCultivo, true, model);
        return "umbrales/lista";
    }

    @PostMapping("/cultivos/{idCultivo:\\d+}/umbrales/{idUmbral:\\d+}/eliminar")
    @PreAuthorize("@permisos.puedeAdministrar(#idCultivo, authentication)")
    public String eliminar(@PathVariable Integer idCultivo, @PathVariable Integer idUmbral,
                           RedirectAttributes flash) {
        umbrales.eliminar(idCultivo, idUmbral);
        flash.addFlashAttribute("mensaje", "Umbral eliminado");
        return "redirect:/cultivos/" + idCultivo + "/umbrales";
    }

    private void prepararVista(Integer idCultivo, boolean puedeAdministrar, Model model) {
        model.addAttribute("cultivo", cultivos.buscar(idCultivo));
        model.addAttribute("umbrales", umbrales.listar(idCultivo));
        model.addAttribute("tiposSensor", umbrales.tiposSensor());
        model.addAttribute("puedeAdministrar", puedeAdministrar);
    }
}
