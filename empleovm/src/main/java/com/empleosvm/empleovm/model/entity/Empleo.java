package com.empleosvm.empleovm.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "empleos")
@Getter @Setter
public class Empleo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;


    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(min = 10, message = "La descripción debe tener al menos 10 caracteres")
    private String descripcion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotBlank(message = "La ubicación es obligatoria (ej: Villa María)")
    private String ubicacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String imagenUrl;

    private Double sueldo;

   @OneToMany(mappedBy = "empleo", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Postulacion> postulaciones = new ArrayList<>();

    
}