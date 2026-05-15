package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.service.interfaces.IEmpleoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/empleos")
@CrossOrigin(origins = "*")
public class EmpleoController {

    @Autowired
    private EmpleoRepository empleoRepository;

    @Autowired
    private IEmpleoService empleoService;

    // ─── 1. Listar todos ──────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<EmpleoResponseDTO>> listar() {
        return ResponseEntity.ok(empleoService.listarTodos());
    }

    // ─── 2. Buscar por ID ─────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            EmpleoResponseDTO empleo = empleoService.buscarPorId(id);
            if (empleo != null) return ResponseEntity.ok(empleo);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "El empleo con ID " + id + " no existe."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 3. Búsqueda avanzada (MEJORADO) ─────────────────────────────────────
    /**
     * Parámetros opcionales:
     *   titulo       → búsqueda por texto
     *   ubicacion    → filtro por ciudad/barrio
     *   sueldoMin    → sueldo mínimo
     *   sueldoMax    → sueldo máximo
     *   soloActivos  → true/false (default true)
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<EmpleoResponseDTO>> buscar(
            @RequestParam(required = false, defaultValue = "") String titulo,
            @RequestParam(required = false, defaultValue = "") String ubicacion,
            @RequestParam(required = false) Double sueldoMin,
            @RequestParam(required = false) Double sueldoMax,
            @RequestParam(required = false, defaultValue = "true") boolean soloActivos) {

        List<EmpleoResponseDTO> todos = titulo.isBlank()
                ? empleoService.listarTodos()
                : empleoService.buscarPorTitulo(titulo);

        List<EmpleoResponseDTO> filtrados = todos.stream()
                .filter(e -> !soloActivos || e.isActivo())
                .filter(e -> ubicacion.isBlank() ||
                        (e.getUbicacion() != null &&
                         e.getUbicacion().toLowerCase().contains(ubicacion.toLowerCase())))
                .filter(e -> sueldoMin == null || (e.getSueldo() != null && e.getSueldo() >= sueldoMin))
                .filter(e -> sueldoMax == null || (e.getSueldo() != null && e.getSueldo() <= sueldoMax))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtrados);
    }

    // ─── 4. Publicar con foto ─────────────────────────────────────────────────
    @PostMapping("/con-foto")
    public ResponseEntity<?> publicarConFoto(
            @RequestParam("titulo")      String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("ubicacion")   String ubicacion,
            @RequestParam("empresa")     String empresa,
            @RequestParam("idUsuario")   Long idUsuario,
            @RequestParam("sueldo")      Double sueldo,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        // Validaciones básicas
        if (titulo == null || titulo.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "El título es obligatorio."));
        if (descripcion == null || descripcion.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "La descripción es obligatoria."));
        if (sueldo == null || sueldo <= 0)
            return ResponseEntity.badRequest().body(Map.of("error", "El sueldo debe ser mayor a 0."));

        try {
            String nombreArchivo = null;

            if (archivo != null && !archivo.isEmpty()) {
                // Validar tipo de archivo
                String contentType = archivo.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Solo se permiten imágenes (jpg, png, gif)."));
                }

                String nombreOriginal = archivo.getOriginalFilename() != null
                        ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "foto.jpg";
                nombreArchivo = System.currentTimeMillis() + "_" + nombreOriginal;

                Path rutaDirectorio = Paths.get("uploads", "fotos").toAbsolutePath();
                if (!Files.exists(rutaDirectorio)) Files.createDirectories(rutaDirectorio);

                Files.copy(archivo.getInputStream(),
                        rutaDirectorio.resolve(nombreArchivo),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            EmpleoRequestDTO dto = new EmpleoRequestDTO();
            dto.setTitulo(titulo.trim());
            dto.setDescripcion(descripcion.trim());
            dto.setUbicacion(ubicacion.trim());
            dto.setEmpresa(empresa.trim());
            dto.setIdUsuario(idUsuario);
            dto.setSueldo(sueldo);
            dto.setImagenUrl(nombreArchivo);

            EmpleoResponseDTO resultado = empleoService.publicarEmpleo(dto);
            return new ResponseEntity<>(resultado, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al publicar: " + e.getMessage()));
        }
    }

    // ─── 5. Publicar sin foto (JSON) ──────────────────────────────────────────
    @PostMapping
    public ResponseEntity<EmpleoResponseDTO> publicar(@RequestBody EmpleoRequestDTO dto) {
        return new ResponseEntity<>(empleoService.publicarEmpleo(dto), HttpStatus.CREATED);
    }

    // ─── 6. Eliminar ──────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            empleoService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Empleo eliminado correctamente."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 7. Cambiar estado activo/pausado ─────────────────────────────────────
    @PatchMapping("/{id}/cambiar-estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id) {
        try {
            Empleo e = empleoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No existe el empleo con ID: " + id));
            e.setActivo(!e.isActivo());
            empleoRepository.save(e);
            return ResponseEntity.ok(Map.of(
                    "nuevoEstado", e.isActivo(),
                    "mensaje", e.isActivo() ? "Empleo activado" : "Empleo pausado"
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}