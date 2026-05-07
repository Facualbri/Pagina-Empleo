package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Este método nos servirá para el Login más adelante
    Optional<Usuario> findByEmail(String email);
}