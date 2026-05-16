package com.empleosvm.empleovm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "empleos")
@Getter
@Setter
public class Empleo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 3, max = 120, message = "El título debe tener entre 3 y 120 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(min = 10, message = "La descripción debe tener al menos 10 caracteres")
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    private String ubicacion;

    @Positive(message = "El sueldo debe ser mayor a 0")
    private Double sueldo;

    private String imagenUrl;

    // ── Fechas ────────────────────────────────────────────────────────────────

    /** Se setea automáticamente al persistir */
    private LocalDateTime fechaPublicacion;

    /**
     * Opcional: si se define, el aviso se pausa automáticamente cuando
     * la fecha actual supera este valor (chequeado al listar en el service).
     */
    private LocalDateTime fechaVencimiento;

    @PrePersist
    protected void onCreate() {
        fechaPublicacion = LocalDateTime.now();
        if (!activo)
            activo = true;
    }

    // ── Estado ────────────────────────────────────────────────────────────────
    private boolean activo = true;

    // ── Contador de visitas ───────────────────────────────────────────────────
    /** Se incrementa cada vez que un candidato abre el detalle del aviso. */
    @Column(name = "vistas", nullable = false)
    private long vistas = 0;

    // ── Relaciones ────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @JsonIgnore
    @OneToMany(mappedBy = "empleo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Postulacion> postulaciones = new ArrayList<>();
}