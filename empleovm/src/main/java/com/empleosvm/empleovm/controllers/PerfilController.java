package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.mapper.UsuarioMapper;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@RequestMapping("/api/perfil")
@CrossOrigin(origins = "*")
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioMapper usuarioMapper;

    // ─── GET: obtener perfil completo ─────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPerfil(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(u -> ResponseEntity.ok(usuarioMapper.toResponseDTO(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null));
    }

    // ─── PATCH: actualizar datos de perfil (texto) ────────────────────────────
    @PatchMapping("/{id}/datos")
    public ResponseEntity<?> actualizarDatos(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (datos.containsKey("nombre") && !datos.get("nombre").isBlank())
            usuario.setNombre(datos.get("nombre").trim());

        if (datos.containsKey("descripcion"))
            usuario.setDescripcion(datos.get("descripcion").trim());

        if (datos.containsKey("experiencia"))
            usuario.setExperiencia(datos.get("experiencia").trim());

        if (datos.containsKey("telefono"))
            usuario.setTelefono(datos.get("telefono").trim());

        if (datos.containsKey("localidad"))
            usuario.setLocalidad(datos.get("localidad").trim());

        // Email con validación básica
        if (datos.containsKey("email") && datos.get("email").contains("@")) {
            String nuevoEmail = datos.get("email").trim().toLowerCase();
            // Verificar que no esté en uso por otro usuario
            usuarioRepository.findByEmail(nuevoEmail).ifPresent(otro -> {
                if (!otro.getId().equals(id))
                    throw new RuntimeException("Ese email ya está en uso.");
            });
            usuario.setEmail(nuevoEmail);
        }

        UsuarioResponseDTO dto = usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
        return ResponseEntity.ok(dto);
    }

    // ─── POST: subir/actualizar foto de perfil ────────────────────────────────
    @PostMapping("/{id}/foto")
    public ResponseEntity<?> subirFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {

        if (foto == null || foto.isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se recibió ninguna imagen."));

        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se aceptan imágenes (JPG, PNG, WEBP)."));

        // Tamaño máximo 5 MB
        if (foto.getSize() > 5 * 1024 * 1024)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La imagen no puede superar los 5 MB."));

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        try {
            // Limpiar nombre y armar ruta
            String ext = foto.getOriginalFilename() != null &&
                         foto.getOriginalFilename().contains(".")
                    ? foto.getOriginalFilename().substring(foto.getOriginalFilename().lastIndexOf("."))
                    : ".jpg";
            String nombreArchivo = "perfil_" + id + "_" + System.currentTimeMillis() + ext;

            Path directorio = Paths.get("uploads", "fotoPerfil").toAbsolutePath();
            if (!Files.exists(directorio)) Files.createDirectories(directorio);

            // Eliminar foto anterior si existe
            if (usuario.getFotoPerfil() != null) {
                Path fotoAnterior = directorio.resolve(usuario.getFotoPerfil());
                Files.deleteIfExists(fotoAnterior);
            }

            Files.copy(foto.getInputStream(), directorio.resolve(nombreArchivo),
                    StandardCopyOption.REPLACE_EXISTING);

            usuario.setFotoPerfil(nombreArchivo);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Foto actualizada correctamente.",
                    "fotoPerfil", nombreArchivo
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen: " + e.getMessage()));
        }
    }

    // ─── PATCH: cambiar contraseña ────────────────────────────────────────────
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String actual  = body.get("passwordActual");
        String nueva   = body.get("passwordNueva");
        String confirm = body.get("passwordConfirm");

        if (actual == null || nueva == null || confirm == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Completá todos los campos de contraseña."));

        if (!nueva.equals(confirm))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Las contraseñas nuevas no coinciden."));

        if (nueva.length() < 6)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La nueva contraseña debe tener al menos 6 caracteres."));

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!usuario.getPassword().equals(actual))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La contraseña actual no es correcta."));

        usuario.setPassword(nueva);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }
}