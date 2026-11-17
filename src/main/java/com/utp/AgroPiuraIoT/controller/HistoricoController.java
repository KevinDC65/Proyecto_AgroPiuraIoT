package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.HistoricoService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Histórico de lecturas por cultivo y rango de fechas (RF-08). */
@Controller
public class HistoricoController {

    private final HistoricoService historico;
    private final CultivoService cultivos;

    public HistoricoController(HistoricoService historico, CultivoService cultivos) {
        this.historico = historico;
        this.cultivos = cultivos;
    }

    /** Punto de entrada del menú: abre el histórico del primer cultivo del usuario. */
    @GetMapping("/historico")
    public String inicio(Authentication auth) {
        List<Cultivo> propios = cultivos.listarPara(auth.getName());
        return propios.isEmpty()
                ? "redirect:/cultivos"
                : "redirect:/cultivos/" + propios.get(0).getIdCultivo() + "/historico";
    }

    @GetMapping("/cultivos/{idCultivo:\\d+}/historico")
    @PreAuthorize("@permisos.puedeVer(#idCultivo, authentication)")
    public String vista(@PathVariable Integer idCultivo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        Model model) {
        model.addAttribute("cultivo", cultivos.buscar(idCultivo));
        // La vista reutiliza el filtro en los inputs y en el enlace de exportación.
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        try {
            model.addAttribute("puntos", historico.puntos(idCultivo, desde, hasta));
            model.addAttribute("resumen", historico.resumen(idCultivo, desde, hasta));
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("puntos", List.of());
            model.addAttribute("resumen", List.of());
        }
        return "historico/lista";
    }

    @GetMapping("/cultivos/{idCultivo:\\d+}/historico/exportar.xlsx")
    @PreAuthorize("@permisos.puedeVer(#idCultivo, authentication)")
    public ResponseEntity<byte[]> exportar(@PathVariable Integer idCultivo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws IOException {
        byte[] archivo = historico.exportarExcel(idCultivo, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("historico-cultivo-" + idCultivo + ".xlsx")
                                .build().toString())
                .contentType(Descargas.EXCEL)
                .body(archivo);
    }
}
