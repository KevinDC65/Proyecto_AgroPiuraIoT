package com.utp.AgroPiuraIoT.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El recurso pedido (cultivo, umbral, alerta, notificación...) no existe o no
 * pertenece al usuario. Spring MVC la traduce a 404 y Spring Boot muestra
 * {@code templates/error/404.html}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " no encontrado: " + id);
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
