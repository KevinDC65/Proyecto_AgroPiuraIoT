package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UsuarioService usuarios;

    public AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/registro")
    String formularioRegistro(Model model) {
        if (!model.containsAttribute("registro")) {
            model.addAttribute("registro", new RegistroRequest("", "", "", ""));
        }
        return "registro";
    }

    @PostMapping("/registro")
    String registrar(@Valid @ModelAttribute("registro") RegistroRequest request,
                     BindingResult errores) {
        if (errores.hasErrors()) {
            return "registro";
        }
        try {
            usuarios.registrar(request);
        } catch (IllegalArgumentException e) {
            errores.reject("registro", e.getMessage());
            return "registro";
        }
        return "redirect:/login?registrado";
    }
}
