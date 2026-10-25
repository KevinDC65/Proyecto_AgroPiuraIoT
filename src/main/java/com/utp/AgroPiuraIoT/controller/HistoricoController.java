package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.model.dto.LecturaPunto;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.service.HistoricoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HistoricoController {

    private final HistoricoService historico;
    private final CultivoRepository cultivos;

    public HistoricoController(HistoricoService historico, CultivoRepository cultivos) {
        this.historico = historico;
        this.cultivos = cultivos;
    }

    /** Punto de entrada del menú: abre el histórico del primer cultivo registrado. */
    @GetMapping("/historico")
    String inicio() {
        List<Cultivo> todos = cultivos.listar();
        return todos.isEmpty()
                ? "redirect:/cultivos"
                : "redirect:/cultivos/" + todos.get(0).getIdCultivo() + "/historico";
    }

    @GetMapping("/cultivos/{idCultivo}/historico")
    String vista(@PathVariable Integer idCultivo,
                 @RequestParam(required = false) LocalDate desde,
                 @RequestParam(required = false) LocalDate hasta,
                 Model model) {
        model.addAttribute("cultivo", cultivos.buscar(idCultivo));
        model.addAttribute("lecturas", historico.consultar(idCultivo, desde, hasta));
        model.addAttribute("puntos", historico.puntos(idCultivo, desde, hasta));
        return "historico/lista";
    }

    @GetMapping("/cultivos/{idCultivo}/historico/exportar.xlsx")
    ResponseEntity<byte[]> exportar(@PathVariable Integer idCultivo,
                                    @RequestParam(required = false) LocalDate desde,
                                    @RequestParam(required = false) LocalDate hasta) throws Exception {
        byte[] archivo = historico.exportarExcel(idCultivo, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("historico-cultivo-" + idCultivo + ".xlsx")
                                .build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }
}
