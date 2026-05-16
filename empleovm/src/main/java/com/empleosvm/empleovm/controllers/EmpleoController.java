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
import java.time.LocalDateTime;
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

    // ─── 1. Listar todos (con auto-pausa por vencimiento) ────────────────────
    @GetMapping
    public ResponseEntity<List<EmpleoResponseDTO>> listar() {
        pausarVencidos();
        return ResponseEntity.ok(empleoService.listarTodos());
    }

    // ─── 2. Buscar por ID (registra vista) ───────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            empleoRepository.findById(id).ifPresent(e -> {
                e.setVistas(e.getVistas() + 1);
                empleoRepository.save(e);
            });
            EmpleoResponseDTO empleo = empleoService.buscarPorId(id);
            if (empleo != null)
                return ResponseEntity.ok(empleo);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "El empleo con ID " + id + " no existe."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 3. Búsqueda avanzada ─────────────────────────────────────────────────
    @GetMapping("/buscar")
    public ResponseEntity<List<EmpleoResponseDTO>> buscar(
            @RequestParam(required = false, defaultValue = "") String titulo,
            @RequestParam(required = false, defaultValue = "") String ubicacion,
            @RequestParam(required = false) Double sueldoMin,
            @RequestParam(required = false) Double sueldoMax,
            @RequestParam(required = false, defaultValue = "true") boolean soloActivos) {

        pausarVencidos();

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
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("ubicacion") String ubicacion,
            @RequestParam("empresa") String empresa,
            @RequestParam("idUsuario") Long idUsuario,
            @RequestParam("sueldo") Double sueldo,
            @RequestParam(value = "fechaVencimiento", required = false) String fechaVencimientoStr,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        if (titulo == null || titulo.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "El título es obligatorio."));
        if (descripcion == null || descripcion.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "La descripción es obligatoria."));
        if (sueldo == null || sueldo <= 0)
            return ResponseEntity.badRequest().body(Map.of("error", "El sueldo debe ser mayor a 0."));

        try {
            String nombreArchivo = null;

            if (archivo != null && !archivo.isEmpty()) {
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
                if (!Files.exists(rutaDirectorio))
                    Files.createDirectories(rutaDirectorio);
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

            if (fechaVencimientoStr != null && !fechaVencimientoStr.isBlank()) {
                try {
                    String fv = fechaVencimientoStr.contains("T")
                            ? fechaVencimientoStr
                            : fechaVencimientoStr + "T00:00:00";
                    dto.setFechaVencimiento(LocalDateTime.parse(fv));
                } catch (Exception ignored) {
                }
            }

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

    // ─── 6. Editar vacante existente ──────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> editarEmpleo(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Empleo empleo = empleoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No existe el empleo con ID: " + id));

            if (body.containsKey("titulo")) {
                String t = body.get("titulo").toString().trim();
                if (t.isBlank())
                    return ResponseEntity.badRequest().body(Map.of("error", "El título no puede estar vacío."));
                empleo.setTitulo(t);
            }
            if (body.containsKey("descripcion")) {
                String d = body.get("descripcion").toString().trim();
                if (d.isBlank())
                    return ResponseEntity.badRequest().body(Map.of("error", "La descripción no puede estar vacía."));
                empleo.setDescripcion(d);
            }
            if (body.containsKey("ubicacion")) {
                empleo.setUbicacion(body.get("ubicacion").toString().trim());
            }
            if (body.containsKey("sueldo")) {
                try {
                    double s = Double.parseDouble(body.get("sueldo").toString());
                    if (s <= 0)
                        return ResponseEntity.badRequest().body(Map.of("error", "El sueldo debe ser mayor a 0."));
                    empleo.setSueldo(s);
                } catch (NumberFormatException ex) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El sueldo debe ser un número válido."));
                }
            }
            if (body.containsKey("fechaVencimiento")) {
                Object fv = body.get("fechaVencimiento");
                if (fv == null || fv.toString().isBlank()) {
                    empleo.setFechaVencimiento(null);
                } else {
                    try {
                        String fvStr = fv.toString().contains("T") ? fv.toString() : fv.toString() + "T00:00:00";
                        LocalDateTime ldt = LocalDateTime.parse(fvStr);
                        empleo.setFechaVencimiento(ldt);
                        // Si la nueva fecha es futura, reactivar
                        if (ldt.isAfter(LocalDateTime.now()))
                            empleo.setActivo(true);
                    } catch (Exception ex) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Formato de fecha inválido. Usar YYYY-MM-DD."));
                    }
                }
            }

            empleoRepository.save(empleo);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Vacante actualizada correctamente.",
                    "id", empleo.getId(),
                    "titulo", empleo.getTitulo()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + ex.getMessage()));
        }
    }

    // ─── 7. Registrar vista manualmente ──────────────────────────────────────
    @PostMapping("/{id}/vista")
    public ResponseEntity<Map<String, Object>> registrarVista(@PathVariable Long id) {

        return empleoRepository.findById(id)
                .map(e -> {
                    e.setVistas(e.getVistas() + 1);
                    empleoRepository.save(e);

                    return ResponseEntity.ok(
                            Map.of("vistas", (Object) e.getVistas()));
                })
                .orElse(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", (Object) "Empleo no encontrado.")));
    }

    // ─── 8. Eliminar ──────────────────────────────────────────────────────────
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

    // ─── 9. Cambiar estado activo/pausado ─────────────────────────────────────
    @PatchMapping("/{id}/cambiar-estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id) {
        try {
            Empleo e = empleoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No existe el empleo con ID: " + id));
            e.setActivo(!e.isActivo());
            empleoRepository.save(e);
            return ResponseEntity.ok(Map.of(
                    "nuevoEstado", e.isActivo(),
                    "mensaje", e.isActivo() ? "Empleo activado" : "Empleo pausado"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    // ─── Helper: pausar automáticamente los avisos vencidos ──────────────────
    private void pausarVencidos() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            List<Empleo> vencidos = empleoRepository.findByActivoTrue().stream()
                    .filter(e -> e.getFechaVencimiento() != null && e.getFechaVencimiento().isBefore(ahora))
                    .collect(Collectors.toList());
            if (!vencidos.isEmpty()) {
                vencidos.forEach(e -> e.setActivo(false));
                empleoRepository.saveAll(vencidos);
            }
        } catch (Exception ignored) {
        }
    }
}