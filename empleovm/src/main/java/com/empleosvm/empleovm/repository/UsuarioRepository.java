package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por email (para login y validación de duplicados)
    Optional<Usuario> findByEmail(String email);

    // Verificar si existe un email (útil en validaciones)
    boolean existsByEmail(String email);

    // Listar solo empresas
    List<Usuario> findByTipo(Usuario.Rol tipo);

    List<Usuario> findByEstadoSolicitud(String estadoSolicitud);
}