package com.empleosvm.empleovm.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulaciones")
@Data
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario postulante; // El vecino que busca trabajo

    @ManyToOne
    @JoinColumn(name = "empleo_id")
    private Empleo empleo; // El aviso al que se postula

    private LocalDateTime fechaPostulacion;

    @PrePersist
    protected void onCreate() {
        fechaPostulacion = LocalDateTime.now();
    }
}