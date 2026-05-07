package com.empleosvm.empleovm.dto.request;

import lombok.Data;

@Data
public class UsuarioRequestDTO{
    private String nombre;
    private String email;
    private String password;
    private String tipo; // "ROLE_USER" o "ROLE_EMPRESA"

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
