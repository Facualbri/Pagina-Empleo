package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Postulacion;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.repository.PostulacionRepository;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/postulaciones")
public class PostulacionController {

    @Autowired
    private PostulacionRepository postulacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleoRepository empleoRepository;

    // ─── 1. Postularse a un empleo ────────────────────────────────────────────
    @PostMapping("/aplicar")
    public ResponseEntity<?> postular(
            @RequestParam("idUsuario") Long idUsuario,
            @RequestParam("idEmpleo") Long idEmpleo,
            @RequestParam("archivoCv") MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debés adjuntar tu CV para postularte."));
        }

        String contentType = archivo.getContentType();
        if (contentType == null ||
                (!contentType.equals("application/pdf") && !contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se aceptan archivos PDF, JPG o PNG."));
        }

        if (postulacionRepository.existsByPostulanteIdAndEmpleoId(idUsuario, idEmpleo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ya te postulaste a este empleo anteriormente."));
        }

        Usuario postulante = usuarioRepository.findById(idUsuario).orElse(null);
        Empleo empleo = empleoRepository.findById(idEmpleo).orElse(null);

        if (postulante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado. Cerrá sesión e ingresá de nuevo."));
        }
        if (empleo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "El empleo no existe o fue eliminado."));
        }
        if (!empleo.isActivo()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Esta vacante está pausada y no acepta postulaciones."));
        }

        try {
            String nombreLimpio = archivo.getOriginalFilename() != null
                    ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "cv.pdf";
            String nombreArchivo = UUID.randomUUID().toString() + "_" + nombreLimpio;

            Path rutaCvs = Paths.get("uploads", "cvs").toAbsolutePath();
            if (!Files.exists(rutaCvs)) Files.createDirectories(rutaCvs);
            Files.copy(archivo.getInputStream(), rutaCvs.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);

            Postulacion nueva = new Postulacion();
            nueva.setPostulante(postulante);
            nueva.setEmpleo(empleo);
            nueva.setArchivoCv(nombreArchivo);
            postulacionRepository.save(nueva);

            return ResponseEntity.ok(Map.of("mensaje", "¡Postulación registrada con éxito!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el CV: " + e.getMessage()));
        }
    }

    // ─── 2. Ver CV de un postulante ──────────────────────────────────────────
    @GetMapping("/cv/{filename}")
    public ResponseEntity<Resource> verCV(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads", "cvs").toAbsolutePath().resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── 3. Ver postulantes de un empleo (para la empresa) ───────────────────
    @GetMapping("/por-empleo/{idEmpleo}")
    public ResponseEntity<?> listarPorEmpleo(@PathVariable Long idEmpleo) {
        try {
            List<Postulacion> lista = postulacionRepository.findByEmpleoId(idEmpleo);
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener postulantes: " + e.getMessage()));
        }
    }

    // ─── 4. Ver mis postulaciones (para el usuario) ───────────────────────────
    @GetMapping("/mis-postulaciones/{idUsuario}")
    public ResponseEntity<?> obtenerMisPostulaciones(@PathVariable Long idUsuario) {
        try {
            List<Postulacion> lista = postulacionRepository.findByPostulanteId(idUsuario);
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener tus postulaciones: " + e.getMessage()));
        }
    }

    // ─── 5. Marcar como visto (empresa lee el CV) ─────────────────────────────
    @PatchMapping("/{id}/marcar-visto")
    public ResponseEntity<?> marcarComoVisto(@PathVariable Long id) {
        return postulacionRepository.findById(id)
                .map(p -> {
                    p.setVisto(true);
                    postulacionRepository.save(p);
                    return ResponseEntity.ok(Map.of("mensaje", "Marcado como visto."));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Postulación no encontrada.")));
    }

    // ─── 6. Eliminar postulación (el usuario retira su candidatura) ───────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!postulacionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Postulación no encontrada."));
        }
        postulacionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Postulación eliminada correctamente."));
    }

    // ─── 7. Cambiar estado del candidato ──────────────────────────────────────
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstadoCandidato(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String nuevoEstado = body.get("estado");
        List<String> estadosValidos = List.of("PENDIENTE", "EN_PROCESO", "CONTACTADO", "DESCARTADO");

        if (nuevoEstado == null || !estadosValidos.contains(nuevoEstado)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Estado inválido."));
        }

        return postulacionRepository.findById(id).map(p -> {
            p.setEstadoCandidato(nuevoEstado);
            p.setVisto(true);
            postulacionRepository.save(p);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Estado actualizado.",
                    "estado", nuevoEstado));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Postulación no encontrada.")));
    }
}