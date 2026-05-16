package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByEstadoSolicitud(String estadoSolicitud);

    Optional<Usuario> findByRefreshToken(String refreshToken);
}