package com.empleosvm.empleovm.dto.request;

import lombok.Data;

@Data
public class EmpleoRequestDTO {

    private String titulo;
    private String descripcion;
    private String empresa;
    private String ubicacion;
    private Double sueldo;
    private Long   idUsuario;
    private String imagenUrl;
}