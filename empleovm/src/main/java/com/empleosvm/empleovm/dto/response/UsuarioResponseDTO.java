package com.empleosvm.empleovm.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para Usuario.
 * Incluye el campo refreshToken para devolverlo al login (y sólo al login).
 * En el resto de respuestas el campo será null y no se serializa.
 */
@Data
public class UsuarioResponseDTO {

    private Long id;
    private String nombre;
    private String email;
    private String tipo;

    // Campos de perfil
    private String fotoPerfil;
    private String descripcion;
    private String experiencia;
    private String telefono;
    private String localidad;
    private String estadoSolicitud;

    private LocalDateTime fechaRegistro;

    // Tokens — sólo presentes en la respuesta del login
    private String token;         // access token (JWT, 8 horas)
    private String refreshToken;  // refresh token opaco (30 días)
}