package com.empleosvm.empleovm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpleoResponseDTO {

    private Long   id;
    private String titulo;
    private String descripcion;
    private String empresa;
    private String ubicacion;
    private String imagenUrl;
    private Double sueldo;
    private Long   idUsuario;
    private boolean activo;
    private long   cantidadPostulantes;

    // Nuevo: fecha de publicación para ordenar/mostrar "Publicado hace X días"
    private LocalDateTime fechaPublicacion;
}