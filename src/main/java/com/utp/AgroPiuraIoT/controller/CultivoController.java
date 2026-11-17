package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.security.PermisosCultivo;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.SimuladorService;
import com.utp.AgroPiuraIoT.util.excel.CultivoExcelExporter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

/**
 * Cultivos y nodos (RF-02). Cada usuario ve solo sus cultivos; editar,
 * eliminar y gestionar nodos exige ser administrador de ese cultivo.
 */
@Controller
@RequestMapping("/cultivos")
public class CultivoController {

    private final CultivoService cultivos;
    private final SimuladorService simulador;
    private final PermisosCultivo permisos;

    public CultivoController(CultivoService cultivos, SimuladorService simulador,
                             PermisosCultivo permisos) {
        this.cultivos = cultivos;
        this.simulador = simulador;
        this.permisos = permisos;
    }

    @GetMapping
    public String lista(Authentication auth, Model model) {
        model.addAttribute("cultivos", cultivos.listarPara(auth.getName()));
        model.addAttribute("cultivosAdministrados", cultivos.idsAdministradosPor(auth.getName()));
        return "cultivos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cultivoId", null);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form",
                    new CultivoForm("", "", "", null, (BigDecimal) null, (BigDecimal) null));
        }
        return "cultivos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") CultivoForm form, BindingResult errores,
                        Authentication auth, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("cultivoId", null);
            return "cultivos/form";
        }
        Cultivo creado = cultivos.crear(form, auth.getName());
        return "redirect:/cultivos/" + creado.getIdCultivo();
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("@permisos.puedeVer(#id, authentication)")
    public String detalle(@PathVariable Integer id, Authentication auth, Model model) {
        model.addAttribute("cultivo", cultivos.buscar(id));
        model.addAttribute("puedeAdministrar", permisos.puedeAdministrar(id, auth));
        return "cultivos/detalle";
    }

    @GetMapping("/{id:\\d+}/editar")
    @PreAuthorize("@permisos.puedeAdministrar(#id, authentication)")
    public String editar(@PathVariable Integer id, Model model) {
        Cultivo cultivo = cultivos.buscar(id);
        model.addAttribute("cultivoId", id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CultivoForm(
                    cultivo.getNombre(), cultivo.getTipoCultivo(), cultivo.getUbicacion(),
                    cultivo.getAreaHa(), cultivo.getLatitud(), cultivo.getLongitud()));
        }
        return "cultivos/form";
    }

    @PostMapping("/{id:\\d+}/editar")
    @PreAuthorize("@permisos.puedeAdministrar(#id, authentication)")
    public String actualizar(@PathVariable Integer id,
                             @Valid @ModelAttribute("form") CultivoForm form,
                             BindingResult errores, Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            model.addAttribute("cultivoId", id);
            return "cultivos/form";
        }
        cultivos.actualizar(id, form);
        flash.addFlashAttribute("mensaje", "Cultivo actualizado");
        return "redirect:/cultivos/" + id;
    }

    @PostMapping("/{id:\\d+}/eliminar")
    @PreAuthorize("@permisos.puedeAdministrar(#id, authentication)")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        cultivos.eliminar(id);
        flash.addFlashAttribute("mensaje", "Cultivo eliminado");
        return "redirect:/cultivos";
    }

    @PostMapping("/{id:\\d+}/dispositivos/nuevo")
    @PreAuthorize("@permisos.puedeAdministrar(#id, authentication)")
    public String registrarDispositivo(@PathVariable Integer id, RedirectAttributes flash) {
        var nodo = cultivos.registrarDispositivo(id);
        flash.addFlashAttribute("mensaje", "Nodo #" + nodo.getIdDispositivo() + " registrado");
        return "redirect:/cultivos/" + id;
    }

    @PostMapping("/{id:\\d+}/dispositivos")
    @PreAuthorize("@permisos.puedeAdministrar(#id, authentication)")
    public String asignarDispositivo(@PathVariable Integer id,
                                     @RequestParam(required = false) Integer idDispositivo,
                                     RedirectAttributes flash) {
        try {
            cultivos.asignarDispositivo(id, idDispositivo);
            flash.addFlashAttribute("mensaje", "Nodo #" + idDispositivo + " asignado");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cultivos/" + id;
    }

    @GetMapping("/exportar.xlsx")
    public ResponseEntity<byte[]> exportar(Authentication auth) throws IOException {
        byte[] archivo = CultivoExcelExporter.exportar(cultivos.listarPara(auth.getName()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("cultivos.xlsx").build().toString())
                .contentType(Descargas.EXCEL)
                .body(archivo);
    }

    @PostMapping("/{id:\\d+}/simular")
    @PreAuthorize("@permisos.puedeVer(#id, authentication)")
    public String simular(@PathVariable Integer id, RedirectAttributes flash) {
        int total = simulador.simular(id);
        if (total == 0) {
            flash.addFlashAttribute("error",
                    "El cultivo no tiene nodos activos con sensores. Registra un nodo primero.");
        } else {
            flash.addFlashAttribute("mensaje", total + " lecturas simuladas");
        }
        return "redirect:/cultivos/" + id;
    }
}
