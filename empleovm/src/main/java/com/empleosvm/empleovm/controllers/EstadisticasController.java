package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.repository.PostulacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/estadisticas")
@CrossOrigin(origins = "*")
public class EstadisticasController {

    @Autowired
    private EmpleoRepository empleoRepository;

    @Autowired
    private PostulacionRepository postulacionRepository;

    /**
     * GET /api/estadisticas/empresa/{idUsuario}
     * Devuelve KPIs del panel admin: total avisos, activos, pausados, postulantes totales.
     */
    @GetMapping("/empresa/{idUsuario}")
    public ResponseEntity<Map<String, Object>> getEstadisticas(@PathVariable Long idUsuario) {

        // Todos los empleos de esta empresa
        var todosLosEmpleos = empleoRepository.findAll()
                .stream()
                .filter(e -> e.getUsuario() != null && e.getUsuario().getId().equals(idUsuario))
                .toList();

        long totalAvisos   = todosLosEmpleos.size();
        long activos       = todosLosEmpleos.stream().filter(e -> e.isActivo()).count();
        long pausados      = totalAvisos - activos;
        long totalPostulantes = todosLosEmpleos.stream()
                .mapToLong(e -> e.getPostulaciones() != null ? e.getPostulaciones().size() : 0)
                .sum();

        // Aviso con más postulantes
        String avisoTop = todosLosEmpleos.stream()
                .max((a, b) -> {
                    int sA = a.getPostulaciones() != null ? a.getPostulaciones().size() : 0;
                    int sB = b.getPostulaciones() != null ? b.getPostulaciones().size() : 0;
                    return Integer.compare(sA, sB);
                })
                .map(e -> e.getTitulo())
                .orElse("—");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAvisos", totalAvisos);
        stats.put("activos", activos);
        stats.put("pausados", pausados);
        stats.put("totalPostulantes", totalPostulantes);
        stats.put("avisoMasPopular", avisoTop);

        return ResponseEntity.ok(stats);
    }
}