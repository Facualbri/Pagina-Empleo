package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.repository.PostulacionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EmpleoRepository empleoRepository;
    private final PostulacionRepository postulacionRepository;

    public EstadisticasController(EmpleoRepository empleoRepository,
            PostulacionRepository postulacionRepository) {
        this.empleoRepository = empleoRepository;
        this.postulacionRepository = postulacionRepository;
    }

    @Transactional(readOnly = true)
    @GetMapping("/empresa/{idUsuario}")
    public ResponseEntity<Map<String, Object>> getEstadisticas(@PathVariable Long idUsuario) {

        var todosLosEmpleos = empleoRepository.findByUsuarioId(idUsuario);

        long totalAvisos = todosLosEmpleos.size();
        long activos = todosLosEmpleos.stream().filter(empleo -> empleo.isActivo()).count();
        long pausados = totalAvisos - activos;
        long totalPostulantes = todosLosEmpleos.stream()
                .mapToLong(e -> postulacionRepository.countByEmpleoId(e.getId()))
                .sum();
        long postulantesNuevos = postulacionRepository.countNoVistasParaEmpresa(idUsuario);

        String avisoTop = todosLosEmpleos.stream()
                .max((a, b) -> {
                    long sA = postulacionRepository.countByEmpleoId(a.getId());
                    long sB = postulacionRepository.countByEmpleoId(b.getId());
                    return Long.compare(sA, sB);
                })
                .map(empleo -> empleo.getTitulo())
                .orElse(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAvisos", totalAvisos);
        stats.put("activos", activos);
        stats.put("pausados", pausados);
        stats.put("totalPostulantes", totalPostulantes);
        stats.put("postulantesNuevos", postulantesNuevos);
        stats.put("avisoMasPopular", avisoTop);

        return ResponseEntity.ok(stats);
    }
}