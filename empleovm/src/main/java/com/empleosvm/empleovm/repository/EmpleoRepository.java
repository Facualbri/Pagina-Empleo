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
    
    List<Empleo> findByTituloContainingIgnoreCase(String titulo);

    @Modifying
    @Transactional
    @Query("DELETE FROM Empleo e WHERE e.id = :id")
    void deleteByIdManual(@Param("id") Long id);
}