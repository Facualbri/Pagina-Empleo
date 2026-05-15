package com.empleosvm.empleovm.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "favoritos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_favorito",
        columnNames = {"usuario_id", "empleo_id"}
    )
)
@Getter
@Setter
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"password", "empleosPublicados"})
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleo_id", nullable = false)
    @JsonIgnoreProperties({"postulaciones", "usuario"})
    private Empleo empleo;

    private LocalDateTime fechaGuardado;

    @PrePersist
    protected void onCreate() {
        fechaGuardado = LocalDateTime.now();
    }
}