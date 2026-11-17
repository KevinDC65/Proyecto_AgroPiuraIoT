package com.utp.AgroPiuraIoT.controller;

import org.springframework.http.MediaType;

/** Tipos de contenido de las descargas de la aplicación. */
final class Descargas {

    static final MediaType EXCEL = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private Descargas() {
    }
}
