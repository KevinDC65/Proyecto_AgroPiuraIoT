package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UmbralController {

    private final UmbralService umbrales;
    private final CultivoService cultivos;
    private final TipoSensorRepository tipos;

    public UmbralController(UmbralService umbrales, CultivoService cultivos,
                            TipoSensorRepository tipos) {
        this.umbrales = umbrales;
        this.cultivos = cultivos;
        this.tipos = tipos;
    }

    /** Entrada del menú lateral: abre la configuración del primer cultivo registrado. */
    @GetMapping("/umbrales")
    String inicio() {
        List<Cultivo> todos = cultivos.listar();
        return todos.isEmpty()
                ? "redirect:/cultivos"
                : "redirect:/cultivos/" + todos.get(0).getIdCultivo() + "/umbrales";
    }

    @GetMapping("/cultivos/{idCultivo}/umbrales")
    String lista(@PathVariable Integer idCultivo,
                 @RequestParam(required = false) Integer editar,
                 Model model) {
        model.addAttribute("cultivo", cultivos.buscar(idCultivo));
        model.addAttribute("umbrales", umbrales.listar(idCultivo));
        model.addAttribute("tiposSensor", tipos.findAll());
        if (editar != null) {
            Umbral umbral = umbrales.buscar(idCultivo, editar);
            model.addAttribute("umbralEditado", umbral);
            model.addAttribute("form", new UmbralForm(
                    umbral.getTipoSensor().getIdTipoSensor(),
                    umbral.getValorMinimo(),
                    umbral.getValorMaximo()));
        } else if (!model.containsAttribute("form")) {
            model.addAttribute("form", new UmbralForm(null, null, null));
        }
        return "umbrales/lista";
    }

    @PostMapping("/cultivos/{idCultivo}/umbrales")
    String guardar(@PathVariable Integer idCultivo,
                   @Valid @ModelAttribute("form") UmbralForm form,
                   BindingResult errores,
                   Model model,
                   RedirectAttributes flash) {
        if (form.valorMinimo() != null && form.valorMaximo() != null
                && form.valorMinimo().compareTo(form.valorMaximo()) > 0) {
            errores.rejectValue("valorMinimo", "error.rango",
                    "El valor mínimo no puede ser mayor que el valor máximo");
        }
        if (errores.hasErrors()) {
            model.addAttribute("cultivo", cultivos.buscar(idCultivo));
            model.addAttribute("umbrales", umbrales.listar(idCultivo));
            model.addAttribute("tiposSensor", tipos.findAll());
            return "umbrales/lista";
        }
        try {
            umbrales.guardar(idCultivo, form);
            flash.addFlashAttribute("mensaje", "Umbral configurado exitosamente");
            return "redirect:/cultivos/" + idCultivo + "/umbrales";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("cultivo", cultivos.buscar(idCultivo));
            model.addAttribute("umbrales", umbrales.listar(idCultivo));
            model.addAttribute("tiposSensor", tipos.findAll());
            model.addAttribute("error", ex.getMessage());
            return "umbrales/lista";
        }
    }

    @PostMapping("/cultivos/{idCultivo}/umbrales/{idUmbral}/eliminar")
    String eliminar(@PathVariable Integer idCultivo,
                    @PathVariable Integer idUmbral,
                    RedirectAttributes flash) {
        try {
            umbrales.eliminar(idCultivo, idUmbral);
            flash.addFlashAttribute("mensaje", "Umbral eliminado exitosamente");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cultivos/" + idCultivo + "/umbrales";
    }
}
