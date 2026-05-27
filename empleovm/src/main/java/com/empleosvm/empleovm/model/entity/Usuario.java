package com.empleosvm.empleovm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol tipo;

    // ── Campos de perfil ──────────────────────────────────────────────────────

    private String fotoPerfil;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String experiencia;

    private String telefono;

    private String localidad;

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @JsonIgnore
    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    private LocalDateTime refreshTokenExpiry;

    // ── Password Reset Token ──────────────────────────────────────────────────

    @JsonIgnore
    @Column(name = "reset_token", length = 512)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // ─────────────────────────────────────────────────────────────────────────

    private LocalDateTime fechaRegistro;

    @Column(name = "estado_solicitud")
    private String estadoSolicitud;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        if (tipo == null)
            tipo = Rol.ROLE_USER;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Empleo> empleosPublicados = new ArrayList<>();

    public enum Rol {
        ROLE_USER,
        ROLE_EMPRESA,
        ROLE_ADMIN
    }
}