package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import com.empleosvm.empleovm.service.interfaces.IEmpleoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.empleosvm.empleovm.repository.EmpleoRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/empleos")
@CrossOrigin(origins = "*")
public class EmpleoController {

    @Autowired
    private IEmpleoService empleoService;

    // 1. Listar todos
    @GetMapping
    public ResponseEntity<List<EmpleoResponseDTO>> listar() {
        return ResponseEntity.ok(empleoService.listarTodos());
    }

    // 2. Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            EmpleoResponseDTO empleo = empleoService.buscarPorId(id);
            if (empleo != null) {
                return ResponseEntity.ok(empleo);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Error: El empleo con ID " + id + " no existe.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno: " + e.getMessage());
        }
    }

    // 3. Buscar por título
    @GetMapping("/buscar")
    public ResponseEntity<List<EmpleoResponseDTO>> buscar(
            @RequestParam(value = "titulo", required = false, defaultValue = "") String titulo) {

        if (titulo.isBlank()) {
            return ResponseEntity.ok(empleoService.listarTodos());
        }

        return ResponseEntity.ok(empleoService.buscarPorTitulo(titulo));
    }

    // 4. Publicar con FOTO (CORREGIDO Y OPTIMIZADO)
    @PostMapping("/con-foto")
    public ResponseEntity<?> publicarConFoto(
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("ubicacion") String ubicacion,
            @RequestParam("empresa") String empresa,
            @RequestParam("idUsuario") Long idUsuario,
            @RequestParam("sueldo") Double sueldo,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        try {
            String nombreArchivo = null;

            // 📸 LOGICA DE GUARDADO DE IMAGEN
            if (archivo != null && !archivo.isEmpty()) {
                // Limpiamos el nombre para evitar problemas con espacios
                String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().replace(" ", "_") : "foto.jpg";
                nombreArchivo = System.currentTimeMillis() + "_" + nombreOriginal;

                // Definimos la ruta absoluta hacia /uploads/fotos
                Path rutaDirectorio = Paths.get("uploads", "fotos").toAbsolutePath();

                // Si no existe la carpeta, la creamos
                if (!Files.exists(rutaDirectorio)) {
                    Files.createDirectories(rutaDirectorio);
                }

                Path rutaDestino = rutaDirectorio.resolve(nombreArchivo);
                
                // Guardamos el archivo físico (con StandardCopyOption por seguridad)
                Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);
                
                System.out.println("LOG: Archivo guardado en: " + rutaDestino);
            }

            // 🧱 ARMAR DTO PARA EL SERVICE
            EmpleoRequestDTO dto = new EmpleoRequestDTO();
            dto.setTitulo(titulo);
            dto.setDescripcion(descripcion);
            dto.setUbicacion(ubicacion);
            dto.setEmpresa(empresa);
            dto.setIdUsuario(idUsuario);
            dto.setSueldo(sueldo);
            dto.setImagenUrl(nombreArchivo); // Pasamos el nombre al DTO

            // LLAMADA AL SERVICE (que ya sabe guardar el imagenUrl en la BD)
            EmpleoResponseDTO resultado = empleoService.publicarEmpleo(dto);

            return new ResponseEntity<>(resultado, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al publicar: " + e.getMessage());
        }
    }

    // 5. Publicar sin foto (JSON)
    @PostMapping
    public ResponseEntity<EmpleoResponseDTO> publicar(@RequestBody EmpleoRequestDTO dto) {
        return new ResponseEntity<>(empleoService.publicarEmpleo(dto), HttpStatus.CREATED);
    }

    // 6. Eliminar empleo (Faltaba para que funcione tu admin.js)
    // Agregá esto al final de tu EmpleoController antes de la última llave
@DeleteMapping("/{id}")
public ResponseEntity<?> eliminar(@PathVariable("id") Long id) { // Asegúrate de que el nombre coincida
    System.out.println("ID recibido en el Controller: " + id); // Esto saldrá en tu consola de IntelliJ
    
    try {
        empleoService.eliminar(id);
        return ResponseEntity.ok().body("{\"message\": \"Borrado exitoso\"}");
    } catch (Exception e) {
        e.printStackTrace(); // Esto te dirá el error exacto en la terminal
        return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
}
}