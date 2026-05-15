package com.empleosvm.empleovm.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UsuarioResponseDTO {

    private Long   id;
    private String nombre;
    private String email;
    private String tipo;
    private LocalDateTime fechaRegistro;

    // Perfil
    private String fotoPerfil;
    private String descripcion;
    private String experiencia;
    private String telefono;
    private String localidad;
    private String estadoSolicitud;
}