package com.utp.AgroPiuraIoT.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error esperado causado por datos del usuario que violan una regla de negocio
 * (rango de umbral invertido, correo duplicado, último administrador...).
 * Los controladores la capturan para mostrar el mensaje en el formulario; si se
 * escapa, se responde 400 en lugar de un 500.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
