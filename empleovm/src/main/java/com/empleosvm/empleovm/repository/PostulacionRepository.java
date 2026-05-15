package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    // Todos los postulantes de un empleo
    List<Postulacion> findByEmpleoId(Long empleoId);

    // Todas las postulaciones de un usuario
    List<Postulacion> findByPostulanteId(Long usuarioId);

    // Verificar si ya se postuló (evita duplicados)
    boolean existsByPostulanteIdAndEmpleoId(Long usuarioId, Long empleoId);

    // Contar postulantes de un empleo
    long countByEmpleoId(Long empleoId);

    // Postulaciones no vistas de un empleo (para notificación a empresa)
    List<Postulacion> findByEmpleoIdAndVistoFalse(Long empleoId);

    // Contar postulaciones no vistas de todos los empleos de una empresa
    @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.empleo.usuario.id = :idEmpresa AND p.visto = false")
    long countNoVistasParaEmpresa(@Param("idEmpresa") Long idEmpresa);
}