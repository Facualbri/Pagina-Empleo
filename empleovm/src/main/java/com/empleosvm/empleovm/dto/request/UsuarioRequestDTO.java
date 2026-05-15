package com.empleosvm.empleovm.dto.request;

import lombok.Data;

@Data
public class UsuarioRequestDTO {
    private String nombre;
    private String email;
    private String password;
    private String tipo; // "ROLE_USER" o "ROLE_EMPRESA"
}