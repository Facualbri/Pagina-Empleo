package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.mapper.UsuarioMapper;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.security.JwtService;
import com.empleosvm.empleovm.service.interfaces.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioMapper usuarioMapper;

    // ─── Crear usuario ────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioRequestDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().isBlank())
            return ResponseEntity.badRequest().body("El nombre es obligatorio.");
        if (dto.getEmail() == null || !dto.getEmail().contains("@"))
            return ResponseEntity.badRequest().body("El email no es válido.");
        if (dto.getPassword() == null || dto.getPassword().length() < 6)
            return ResponseEntity.badRequest().body("La contraseña debe tener al menos 6 caracteres.");

        try {
            UsuarioResponseDTO respuesta = usuarioService.guardarUsuario(dto);
            return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("unique"))
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe una cuenta con ese email.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar: " + e.getMessage());
        }
    }

    // ─── Listar todos ─────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    // ─── Buscar por ID ────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
        }
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioRequestDTO loginDTO) {
        if (loginDTO.getEmail() == null || loginDTO.getPassword() == null)
            return ResponseEntity.badRequest().body("Email y contraseña son obligatorios.");

        UsuarioResponseDTO usuarioDTO = usuarioService.autenticar(loginDTO.getEmail(), loginDTO.getPassword());

        if (usuarioDTO != null) {
            // Access token (8 horas)
            String accessToken = jwtService.generarToken(
                    usuarioDTO.getId(), usuarioDTO.getEmail(), usuarioDTO.getTipo());
            usuarioDTO.setToken(accessToken);

            // Refresh token (30 días) — persistido en BD
            String refreshToken = jwtService.generarRefreshToken();
            LocalDateTime expiry = LocalDateTime.now()
                    .plusNanos(jwtService.getRefreshExpirationMs() * 1_000_000L);

            usuarioRepository.findByEmail(loginDTO.getEmail()).ifPresent(usuario -> {
                usuario.setRefreshToken(refreshToken);
                usuario.setRefreshTokenExpiry(expiry);
                usuarioRepository.save(usuario);
            });

            // Devolvemos ambos tokens al frontend
            usuarioDTO.setRefreshToken(refreshToken);

            return ResponseEntity.ok(usuarioDTO);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales inválidas. Verificá tu email y contraseña."));
    }

    // ─── Actualizar perfil ────────────────────────────────────────────────────
    @PatchMapping("/{id}/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody Map<String, String> cambios) {
        try {
            UsuarioResponseDTO actualizado = usuarioService.actualizarPerfil(id, cambios);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ─── Cambiar contraseña ───────────────────────────────────────────────────
    @PatchMapping("/{id}/cambiar-password")
    public ResponseEntity<?> cambiarPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String actual = body.get("passwordActual");
        String nueva = body.get("passwordNueva");

        if (actual == null || nueva == null || nueva.length() < 6)
            return ResponseEntity.badRequest()
                    .body("La nueva contraseña debe tener al menos 6 caracteres.");

        try {
            usuarioService.cambiarPassword(id, actual, nueva);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ─── Switch rol ───────────────────────────────────────────────────────────
    @PutMapping("/{id}/switch-rol")
    public ResponseEntity<?> switchRol(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            if (u.getTipo() == Usuario.Rol.ROLE_USER) {
                u.setTipo(Usuario.Rol.ROLE_EMPRESA);
            } else {
                u.setTipo(Usuario.Rol.ROLE_USER);
            }
            usuarioRepository.save(u);
            return ResponseEntity.ok(Map.of(
                    "nuevoRol", u.getTipo().name(),
                    "mensaje", "Rol actualizado correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── Solicitar cuenta empresa ─────────────────────────────────────────────
    @PutMapping("/{id}/solicitar-empresa")
    public ResponseEntity<?> solicitarEmpresa(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            if ("PENDIENTE".equals(u.getEstadoSolicitud())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ya tenés una solicitud pendiente."));
            }
            if (u.getTipo() == Usuario.Rol.ROLE_EMPRESA) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Tu cuenta ya es de empresa."));
            }
            u.setEstadoSolicitud("PENDIENTE");
            usuarioRepository.save(u);
            return ResponseEntity.ok(Map.of("mensaje", "Solicitud registrada correctamente."));
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado")));
    }

    // ─── Ver solicitudes pendientes ───────────────────────────────────────────
    @GetMapping("/solicitudes-empresa")
    public ResponseEntity<List<UsuarioResponseDTO>> getSolicitudesPendientes() {
        List<Usuario> pendientes = usuarioRepository.findByEstadoSolicitud("PENDIENTE");
        List<UsuarioResponseDTO> dtos = pendientes.stream()
                .map(usuarioMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ─── Aprobar solicitud ────────────────────────────────────────────────────
    @PutMapping("/{id}/aprobar-empresa")
    public ResponseEntity<?> aprobarEmpresa(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            u.setTipo(Usuario.Rol.ROLE_EMPRESA);
            u.setEstadoSolicitud("APROBADO");
            usuarioRepository.save(u);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario aprobado como empresa.",
                    "usuario", u.getNombre(),
                    "email", u.getEmail()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/rechazar-empresa")
    public ResponseEntity<?> rechazarEmpresa(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            u.setEstadoSolicitud("RECHAZADO");
            usuarioRepository.save(u);
            return ResponseEntity.ok(Map.of("mensaje", "Solicitud rechazada."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/generar-hash/{password}")
    public ResponseEntity<?> generarHash(@PathVariable String password) {
        return ResponseEntity.ok(Map.of("hash",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password)));
    }
}