package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.service.DashboardService;
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
    String mapa(Model model) {
        model.addAttribute("resumen", dashboard.resumen());
        return "mapa/lista";
    }
}