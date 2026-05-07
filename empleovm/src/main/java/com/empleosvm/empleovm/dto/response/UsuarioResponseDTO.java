package com.empleosvm.empleovm.dto.response;

import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String email;
    private String tipo;
    
}