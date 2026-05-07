package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Postulacion;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.repository.PostulacionRepository;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/postulaciones")
@CrossOrigin(origins = "*") // Para evitar bloqueos de navegador
public class PostulacionController {

    @Autowired
    private PostulacionRepository postulacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleoRepository empleoRepository;

    // 1. Endpoint para que un usuario se postule
    @PostMapping("/aplicar")
    public ResponseEntity<String> postular(@RequestParam Long idUsuario, @RequestParam Long idEmpleo) {
        
        // Validar si ya existe la postulación para evitar duplicados
        if (postulacionRepository.existsByPostulanteIdAndEmpleoId(idUsuario, idEmpleo)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ya te has postulado a este empleo anteriormente.");
        }

        // Buscar el usuario y el empleo en la base de datos
        Usuario postulante = usuarioRepository.findById(idUsuario)
                .orElse(null);
        Empleo empleo = empleoRepository.findById(idEmpleo)
                .orElse(null);

        if (postulante == null || empleo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario o Empleo no encontrado.");
        }

        // Crear la nueva postulación
        Postulacion nuevaPostulacion = new Postulacion();
        nuevaPostulacion.setPostulante(postulante);
        nuevaPostulacion.setEmpleo(empleo);
        
        postulacionRepository.save(nuevaPostulacion);

        return ResponseEntity.ok("¡Postulación registrada con éxito en Villa María Empleos!");
    }

    // 2. Endpoint para que la empresa vea quiénes se postularon a un empleo suyo
    @GetMapping("/por-empleo/{idEmpleo}")
    public ResponseEntity<List<Postulacion>> listarPorEmpleo(@PathVariable Long idEmpleo) {
        List<Postulacion> lista = postulacionRepository.findByEmpleoId(idEmpleo);
        return ResponseEntity.ok(lista);
    }

    // 3. Endpoint para que el usuario vea sus propias postulaciones
    @GetMapping("/mis-postulaciones/{idUsuario}")
    public ResponseEntity<List<Postulacion>> listarPorUsuario(@PathVariable Long idUsuario) {
        List<Postulacion> lista = postulacionRepository.findByPostulanteId(idUsuario);
        return ResponseEntity.ok(lista);
    }
}