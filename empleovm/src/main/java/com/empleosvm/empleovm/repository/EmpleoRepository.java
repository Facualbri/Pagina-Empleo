package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Empleo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EmpleoRepository extends JpaRepository<Empleo, Long> {

    // Búsqueda por título (insensible a mayúsculas)
    List<Empleo> findByTituloContainingIgnoreCase(String titulo);

    // Solo los activos
    List<Empleo> findByActivoTrue();

    // Los de una empresa en particular
    List<Empleo> findByUsuarioId(Long usuarioId);

    // Los activos de una empresa en particular
    List<Empleo> findByUsuarioIdAndActivoTrue(Long usuarioId);

    // Búsqueda por título y ubicación
    List<Empleo> findByTituloContainingIgnoreCaseAndUbicacionContainingIgnoreCase(
            String titulo, String ubicacion);

    // Delete manual para evitar problemas con cascade
    @Modifying
    @Transactional
    @Query("DELETE FROM Empleo e WHERE e.id = :id")
    void deleteByIdManual(@Param("id") Long id);
}