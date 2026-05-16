package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.repository.PostulacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    @Autowired
    private EmpleoRepository empleoRepository;

    @Autowired
    private PostulacionRepository postulacionRepository;

    /**
     * GET /api/estadisticas/empresa/{idUsuario}
     *
     * Devuelve KPIs del panel empresa:
     *   - totalAvisos
     *   - activos
     *   - pausados
     *   - totalPostulantes
     *   - postulantesNuevos  ← postulaciones con visto=false (sin revisar)
     *   - avisoMasPopular    ← título del aviso con más postulantes
     */
    @GetMapping("/empresa/{idUsuario}")
    public ResponseEntity<Map<String, Object>> getEstadisticas(@PathVariable Long idUsuario) {

        // Todos los empleos de esta empresa
        List<Empleo> todosLosEmpleos = empleoRepository.findAll()
                .stream()
                .filter(e -> e.getUsuario() != null && e.getUsuario().getId().equals(idUsuario))
                .toList();

        long totalAvisos   = todosLosEmpleos.size();
        long activos       = todosLosEmpleos.stream().filter(Empleo::isActivo).count();
        long pausados      = totalAvisos - activos;

        // Total postulantes (todos)
        long totalPostulantes = todosLosEmpleos.stream()
                .mapToLong(e -> e.getPostulaciones() != null ? e.getPostulaciones().size() : 0)
                .sum();

        // Postulantes nuevos (no vistos) — los que la empresa todavía no revisó
        long postulantesNuevos = todosLosEmpleos.stream()
                .flatMap(e -> e.getPostulaciones() != null ? e.getPostulaciones().stream() : java.util.stream.Stream.empty())
                .filter(p -> !p.isVisto())
                .count();

        // Aviso con más postulantes
        String avisoTop = todosLosEmpleos.stream()
                .max((a, b) -> {
                    int sA = a.getPostulaciones() != null ? a.getPostulaciones().size() : 0;
                    int sB = b.getPostulaciones() != null ? b.getPostulaciones().size() : 0;
                    return Integer.compare(sA, sB);
                })
                .map(Empleo::getTitulo)
                .orElse(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAvisos",       totalAvisos);
        stats.put("activos",           activos);
        stats.put("pausados",          pausados);
        stats.put("totalPostulantes",  totalPostulantes);
        stats.put("postulantesNuevos", postulantesNuevos);
        stats.put("avisoMasPopular",   avisoTop);

        return ResponseEntity.ok(stats);
    }
}