package com.empleosvm.empleovm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "postulaciones",
        // Índice único para evitar postulaciones duplicadas a nivel de BD
        uniqueConstraints = @UniqueConstraint(name = "uk_postulacion_usuario_empleo", columnNames = { "usuario_id",
                "empleo_id" }))
@Getter
@Setter
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El candidato que se postula
    @JsonIgnoreProperties({ "postulaciones", "password" })
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario postulante;

    // El empleo al que se postula
    @JsonIgnoreProperties({ "postulaciones", "usuario" })
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleo_id", nullable = false)
    private Empleo empleo;

    // Ruta del archivo CV subido
    @Column(nullable = false)
    private String archivoCv;

    // Fecha automática al crear
    private LocalDateTime fechaPostulacion;

    @PrePersist
    protected void onCreate() {
        fechaPostulacion = LocalDateTime.now();
    }

    // Si la empresa ya vio/leyó esta postulación
    private boolean visto = false;

    @Column(name = "estado_candidato")
    private String estadoCandidato = "PENDIENTE"; // PENDIENTE, EN_PROCESO, CONTACTADO, DESCARTADO
}