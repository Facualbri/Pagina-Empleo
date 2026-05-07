package com.empleosvm.empleovm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmpleoRequestDTO {
    
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 5, max = 100, message = "El título debe tener entre 5 y 100 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotBlank(message = "Debe indicar la empresa")
    private String empresa;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    @NotNull(message = "El sueldo es obligatorio")
    @Positive(message = "El sueldo debe ser un número positivo")
    private Double sueldo; // <--- AGREGADO PARA EL NUEVO CAMPO

    @NotNull(message = "El ID de usuario es obligatorio")
    private Long idUsuario;

    private String imagenUrl; 
}