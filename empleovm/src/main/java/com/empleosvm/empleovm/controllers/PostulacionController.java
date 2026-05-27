package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Postulacion;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.repository.PostulacionRepository;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.service.CloudinaryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/postulaciones")
public class PostulacionController {

    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleoRepository empleoRepository;
    private final CloudinaryService cloudinaryService;

    public PostulacionController(PostulacionRepository postulacionRepository,
            UsuarioRepository usuarioRepository,
            EmpleoRepository empleoRepository,
            CloudinaryService cloudinaryService) {
        this.postulacionRepository = postulacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empleoRepository = empleoRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // ─── 1. Postularse a un empleo ────────────────────────────────────────────
    @Transactional
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

        // Validación por magic bytes
        String validationError = validarMagicBytes(archivo);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
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
            String cvUrl = cloudinaryService.uploadFile(archivo, "cvs");

            Postulacion nueva = new Postulacion();
            nueva.setPostulante(postulante);
            nueva.setEmpleo(empleo);
            nueva.setArchivoCv(cvUrl);
            postulacionRepository.save(nueva);

            return ResponseEntity.ok(Map.of("mensaje", "¡Postulación registrada con éxito!"));

        } catch (Exception e) {
            log.error("Error al procesar el CV para usuario {} empleo {}", idUsuario, idEmpleo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el CV: " + e.getMessage()));
        }
    }

    // ─── 2. Ver CV de un postulante ──────────────────────────────────────────
    @GetMapping("/cv/{filename}")
    public ResponseEntity<?> verCV(@PathVariable String filename) {
        if (filename == null || filename.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // Si es URL de Cloudinary → redirect
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(filename))
                    .build();
        }

        // Si no, asumimos que es un filename de uploads local (legacy)
        return ResponseEntity.notFound().build();
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

    private String validarMagicBytes(MultipartFile archivo) {
        try (InputStream is = archivo.getInputStream()) {
            byte[] magic = new byte[8];
            int bytesRead = is.read(magic, 0, 8);
            if (bytesRead < 4)
                return "Archivo vacío o corrupto.";

            // PDF: %PDF (25 50 44 46)
            if (magic[0] == 0x25 && magic[1] == 0x50 && magic[2] == 0x44 && magic[3] == 0x46)
                return null;
            // JPEG: FF D8 FF
            if (magic[0] == (byte) 0xFF && magic[1] == (byte) 0xD8 && magic[2] == (byte) 0xFF)
                return null;
            // PNG: 89 50 4E 47
            if (magic[0] == (byte) 0x89 && magic[1] == 0x50 && magic[2] == 0x4E && magic[3] == 0x47)
                return null;

            return "Formato de archivo no permitido. Solo PDF, JPG o PNG.";
        } catch (Exception e) {
            log.error("Error al validar magic bytes", e);
            return "Error al validar el archivo.";
        }
    }
}