package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    String dashboard(Model model) {
        model.addAttribute("resumen", dashboard.resumen());
        return "dashboard";
    }
}
