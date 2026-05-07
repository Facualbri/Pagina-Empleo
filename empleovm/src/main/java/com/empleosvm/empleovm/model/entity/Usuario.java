package com.empleosvm.empleovm.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter @Setter // Usamos Lombok para no escribir miles de líneas
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    
    @Column(unique = true)
    private String email;
    
    private String password;

    @Enumerated(EnumType.STRING)
    private Rol tipo; // Puede ser ROLE_USER o ROLE_EMPRESA

    public enum Rol {
        ROLE_USER, ROLE_EMPRESA
    }

    
}