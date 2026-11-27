package com.utp.AgroPiuraIoT.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = PATRON_CONTRASENA,
                message = "La contraseña debe incluir al menos una letra y un número")
        String contrasena,

        @NotBlank(message = "Debes confirmar la contraseña")
        String confirmacion) {

    /** Permite cadena vacía para no duplicar @NotBlank; si tiene texto exige letras y números. */
    public static final String PATRON_CONTRASENA = "^(?:|(?=.*\\p{L})(?=.*\\d).+)$";

    public boolean clavesCoinciden() {
        return contrasena != null && contrasena.equals(confirmacion);
    }
}
