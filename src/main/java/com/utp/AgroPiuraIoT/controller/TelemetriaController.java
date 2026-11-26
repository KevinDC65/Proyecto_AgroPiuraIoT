package com.utp.AgroPiuraIoT.controller;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaResponse;
import com.utp.AgroPiuraIoT.service.TelemetriaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
    public ResponseEntity<TelemetriaResponse> recibirTelemetria(@RequestBody(required = false) TelemetriaRequest request) {
        TelemetriaResponse response = telemetriaService.procesar(request);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<TelemetriaResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(TelemetriaResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<TelemetriaResponse> manejarDatosInvalidos(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(TelemetriaResponse.error(ex.getMessage()));
    }

    /**
     * Dos envíos simultáneos de la misma lectura: el servicio no alcanzó a verlo
     * y el índice único de la BD frenó el segundo (A3). El nodo no debe reintentar.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<TelemetriaResponse> manejarDuplicadoSimultaneo(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(TelemetriaResponse.error("La lectura ya estaba registrada (envío simultáneo); no reintentar"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TelemetriaResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(TelemetriaResponse.error("El cuerpo de la solicitud no es un JSON válido o está vacío"));
    }
}
