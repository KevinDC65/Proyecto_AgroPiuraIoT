package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.service.ClimaService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.DashboardService;
import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboard;
    private final ClimaService clima;
    private final CultivoService cultivos;

    public DashboardController(DashboardService dashboard, ClimaService clima,
                               CultivoService cultivos) {
        this.dashboard = dashboard;
        this.clima = clima;
        this.cultivos = cultivos;
    }

    @GetMapping("/dashboard")
    String dashboard(Model model) {
        model.addAttribute("resumen", dashboard.resumen());
        // RF-06: clima de la primera ubicación registrada. Vacío (sin tarjeta)
        // si no hay cultivo con ubicación, sin red o sin API key.
        Optional<ClimaActual> actual = cultivos.listar().stream()
                .filter(c -> c.getUbicacion() != null && !c.getUbicacion().isBlank())
                .findFirst()
                .flatMap(clima::actual);
        model.addAttribute("clima", actual.orElse(null));
        return "dashboard";
    }
}
