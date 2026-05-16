package com.empleosvm.empleovm.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmpleoRequestDTO {

    private String titulo;
    private String descripcion;
    private String empresa;
    private String ubicacion;
    private Double sueldo;
    private String imagenUrl;
    private Long   idUsuario;

    /**
     * Opcional. Si se envía, el aviso se pausa automáticamente
     * cuando la fecha actual supera este valor.
     * Formato ISO-8601: "2025-08-31T00:00:00"
     */
    private LocalDateTime fechaVencimiento;
}