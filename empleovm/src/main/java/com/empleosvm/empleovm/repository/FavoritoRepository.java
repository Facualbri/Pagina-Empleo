package com.empleosvm.empleovm.repository;

import com.empleosvm.empleovm.model.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByUsuarioId(Long usuarioId);
    Optional<Favorito> findByUsuarioIdAndEmpleoId(Long usuarioId, Long empleoId);
    boolean existsByUsuarioIdAndEmpleoId(Long usuarioId, Long empleoId);
    void deleteByUsuarioIdAndEmpleoId(Long usuarioId, Long empleoId);
}