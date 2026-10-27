package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.SimuladorService;
import com.utp.AgroPiuraIoT.util.excel.CultivoExcelExporter;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cultivos")
public class CultivoController {

    private final CultivoService cultivos;
    private final SimuladorService simulador;

    public CultivoController(CultivoService cultivos, SimuladorService simulador) {
        this.cultivos = cultivos;
        this.simulador = simulador;
    }   

    @GetMapping
    String lista(Model model) {
        model.addAttribute("cultivos", cultivos.listar());
        return "cultivos/lista";
    }

    @GetMapping("/nuevo")
    String nuevo(Model model) {
        model.addAttribute("cultivoId", null);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form",
                    new CultivoForm("", "", "", null, (BigDecimal) null, (BigDecimal) null));
        }
        return "cultivos/form";
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("form") CultivoForm form, BindingResult errores, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("cultivoId", null);
            return "cultivos/form";
        }
        Cultivo creado = cultivos.guardar(form);
        return "redirect:/cultivos/" + creado.getIdCultivo();
    }

    @GetMapping("/{id:\\d+}")
    String detalle(@PathVariable Integer id, Model model) {
        model.addAttribute("cultivo", cultivos.buscar(id));
        return "cultivos/detalle";
    }

    @GetMapping("/{id}/editar")
    String editar(@PathVariable Integer id, Model model) {
        Cultivo cultivo = cultivos.buscar(id);
        model.addAttribute("cultivoId", id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CultivoForm(
                    cultivo.getNombre(), cultivo.getTipoCultivo(), cultivo.getUbicacion(),
                    cultivo.getAreaHa(), cultivo.getLatitud(), cultivo.getLongitud()));
        }
        return "cultivos/form";
    }

    @PostMapping("/{id}/editar")
    String actualizar(@PathVariable Integer id,
                      @Valid @ModelAttribute("form") CultivoForm form,
                      BindingResult errores, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("cultivoId", id);
            return "cultivos/form";
        }
        cultivos.actualizar(id, form);
        return "redirect:/cultivos/" + id;
    }

    @PostMapping("/{id}/eliminar")
    String eliminar(@PathVariable Integer id) {
        cultivos.eliminar(id);
        return "redirect:/cultivos";
    }

    @PostMapping("/{id}/dispositivos/nuevo")
    String registrarDispositivo(@PathVariable Integer id) {
        cultivos.registrarDispositivo(id);
        return "redirect:/cultivos/" + id;
    }

    @PostMapping("/{id}/dispositivos")
    String asignarDispositivo(@PathVariable Integer id,
                              @RequestParam Integer idDispositivo) {
        cultivos.asignarDispositivo(id, idDispositivo);
        return "redirect:/cultivos/" + id;
    }

    @GetMapping("/exportar.xlsx")
    ResponseEntity<byte[]> exportar() throws Exception {
        byte[] archivo = CultivoExcelExporter.exportar(cultivos.listar());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("cultivos.xlsx").build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }

        @PostMapping("/{id}/simular")
    String simular(@PathVariable Integer id, RedirectAttributes flash) {
        int total = simulador.simular(id);
        flash.addFlashAttribute("mensaje", total + " lecturas simuladas");
        return "redirect:/cultivos/" + id;
    }
}
