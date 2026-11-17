package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import com.utp.AgroPiuraIoT.service.ClimaService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.DashboardService;
import java.util.Optional;
import org.springframework.security.core.Authentication;
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
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("resumen", dashboard.resumen(auth.getName()));
        // RF-06: clima de la primera ubicación registrada entre los cultivos del
        // usuario. Vacío (sin tarjeta) si no hay ubicación, sin red o sin API key.
        Optional<ClimaActual> actual = cultivos.listarPara(auth.getName()).stream()
                .filter(c -> c.getUbicacion() != null && !c.getUbicacion().isBlank())
                .findFirst()
                .flatMap(clima::actual);
        model.addAttribute("clima", actual.orElse(null));
        return "dashboard";
    }
}
