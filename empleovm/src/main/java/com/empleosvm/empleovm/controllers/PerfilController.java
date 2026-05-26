package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.mapper.UsuarioMapper;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepository,
            UsuarioMapper usuarioMapper,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
    } // ← AGREGADO: necesario para BCrypt

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

        if (foto.getSize() > 5 * 1024 * 1024)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La imagen no puede superar los 5 MB."));

        String validationError = validarMagicBytesImagen(foto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        try {
            String ext = foto.getOriginalFilename() != null &&
                    foto.getOriginalFilename().contains(".")
                            ? foto.getOriginalFilename().substring(foto.getOriginalFilename().lastIndexOf("."))
                            : ".jpg";
            String nombreArchivo = "perfil_" + id + "_" + System.currentTimeMillis() + ext;

            Path directorio = Paths.get("uploads", "fotoPerfil").toAbsolutePath();
            if (!Files.exists(directorio))
                Files.createDirectories(directorio);

            Files.copy(foto.getInputStream(), directorio.resolve(nombreArchivo),
                    StandardCopyOption.REPLACE_EXISTING);

            usuario.setFotoPerfil(nombreArchivo);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Foto actualizada correctamente.",
                    "fotoPerfil", nombreArchivo));

        } catch (Exception e) {
            log.error("Error al guardar la foto de perfil para usuario {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen: " + e.getMessage()));
        }
    }

    // ─── PATCH: cambiar contraseña ────────────────────────────────────────────
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String actual = body.get("passwordActual");
        String nueva = body.get("passwordNueva");
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

        // ← FIX: usar BCrypt para comparar, no .equals()
        if (!passwordEncoder.matches(actual, usuario.getPassword()))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La contraseña actual no es correcta."));

        // ← FIX: hashear la nueva contraseña antes de guardar
        usuario.setPassword(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }

    private String validarMagicBytesImagen(MultipartFile archivo) {
        try (InputStream is = archivo.getInputStream()) {
            byte[] magic = new byte[4];
            int bytesRead = is.read(magic, 0, 4);
            if (bytesRead < 4)
                return "Archivo vacío o corrupto.";

            if (magic[0] == (byte) 0xFF && magic[1] == (byte) 0xD8 && magic[2] == (byte) 0xFF)
                return null;
            if (magic[0] == (byte) 0x89 && magic[1] == 0x50 && magic[2] == 0x4E && magic[3] == 0x47)
                return null;
            if (magic[0] == 0x52 && magic[1] == 0x49 && magic[2] == 0x46 && magic[3] == 0x46)
                return null;

            return "Formato de imagen no permitido. Solo JPG, PNG o WEBP.";
        } catch (Exception e) {
            log.error("Error al validar magic bytes de imagen", e);
            return "Error al validar la imagen.";
        }
    }
}