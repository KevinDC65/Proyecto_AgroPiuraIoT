package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaResponse;
import com.utp.AgroPiuraIoT.service.TelemetriaService;
import com.utp.AgroPiuraIoT.service.TelemetriaService.LoteInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetria")
public class TelemetriaController {

    private final TelemetriaService telemetriaService;

    public TelemetriaController(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    @PostMapping
    public ResponseEntity<TelemetriaResponse> recibirTelemetria(
            @RequestBody(required = false) TelemetriaRequest request,
            Authentication auth) {
        Integer idDispositivo = auth != null && auth.getPrincipal() instanceof Integer id ? id : null;
        TelemetriaResponse response = telemetriaService.procesar(request, idDispositivo);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<TelemetriaResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(TelemetriaResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(LoteInvalidoException.class)
    public ResponseEntity<TelemetriaResponse> manejarLoteInvalido(LoteInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(TelemetriaResponse.errorConDetalle(
                        ex.getMessage(), ex.getLecturasRechazadas(), ex.getErrores()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<TelemetriaResponse> manejarDatosInvalidos(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(TelemetriaResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TelemetriaResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(TelemetriaResponse.error("El cuerpo de la solicitud no es un JSON válido o está vacío"));
    }
}
