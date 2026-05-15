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

    // Nombre del archivo guardado en uploads/fotoPerfil/
    private String fotoPerfil;

    // "Soy una persona proactiva con 3 años de experiencia en atención al
    // cliente..."
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Experiencia laboral: cargos, empresas, períodos
    @Column(columnDefinition = "TEXT")
    private String experiencia;

    // Teléfono de contacto
    private String telefono;

    // Ciudad o barrio del candidato
    private String localidad;

    // ─────────────────────────────────────────────────────────────────────────

    private LocalDateTime fechaRegistro;

    // ─── Solicitud empresa
    // ────────────────────────────────────────────────────────
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