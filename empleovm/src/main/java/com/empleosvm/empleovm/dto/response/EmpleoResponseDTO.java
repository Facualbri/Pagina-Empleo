package com.empleosvm.empleovm.dto.response;

import lombok.Data;

@Data
public class EmpleoResponseDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String empresa;
    private String ubicacion;
    private String imagenUrl;
    
    // Este campo permite que la tabla de gestión muestre el dinero ofrecido
    private Double sueldo;

    // Este campo es CRÍTICO: es el que usa admin.js para filtrar
    // y mostrar solo las vacantes de la empresa logueada
    private Long idUsuario;

    // Opcional: por si quieres mostrar el nombre de quien publicó en la lista general
    private String nombrePublicador;
}