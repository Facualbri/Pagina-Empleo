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

    // Postulaciones que la empresa aún no revisó (visto = false)
    private long postulantesNuevos;

    // Cantidad de veces que se abrió el detalle del aviso
    private long vistas;

    // Fecha de publicación
    private LocalDateTime fechaPublicacion;

    // Fecha de vencimiento — cuando se alcanza, el aviso se pausa automáticamente
    private LocalDateTime fechaVencimiento;
}