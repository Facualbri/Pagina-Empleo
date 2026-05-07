package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    // Para que la empresa vea quiénes se anotaron en un empleo específico
    List<Postulacion> findByEmpleoId(Long empleoId);

    // Para que el usuario vea a qué cosas se postuló
    List<Postulacion> findByPostulanteId(Long usuarioId);

    // Para evitar que alguien se postule dos veces al mismo laburo
    boolean existsByPostulanteIdAndEmpleoId(Long usuarioId, Long empleoId);
}