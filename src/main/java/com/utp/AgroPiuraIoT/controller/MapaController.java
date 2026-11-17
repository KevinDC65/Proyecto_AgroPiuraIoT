package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapaController {

    private final DashboardService dashboard;

    public MapaController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/mapa")
    public String mapa(Authentication auth, Model model) {
        model.addAttribute("resumen", dashboard.resumen(auth.getName()));
        return "mapa/lista";
    }
}
