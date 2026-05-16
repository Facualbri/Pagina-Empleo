package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints de autenticación complementarios:
 *   POST /api/auth/refresh  → recibe refreshToken, devuelve nuevo accessToken + refreshToken rotado
 *   POST /api/auth/logout   → invalida el refreshToken del usuario
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "refreshToken es obligatorio."));
        }

        Optional<Usuario> opt = usuarioRepository.findByRefreshToken(refreshToken);

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token inválido."));
        }

        Usuario usuario = opt.get();

        // Verificar expiración
        if (usuario.getRefreshTokenExpiry() == null
                || usuario.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            // Limpiar token vencido por seguridad
            usuario.setRefreshToken(null);
            usuario.setRefreshTokenExpiry(null);
            usuarioRepository.save(usuario);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token expirado. Volvé a iniciar sesión."));
        }

        // Refresh token rotation: generamos uno nuevo en cada uso
        String nuevoRefreshToken = jwtService.generarRefreshToken();
        usuario.setRefreshToken(nuevoRefreshToken);
        usuario.setRefreshTokenExpiry(
                LocalDateTime.now().plusNanos(jwtService.getRefreshExpirationMs() * 1_000_000L)
        );
        usuarioRepository.save(usuario);

        // Nuevo access token
        String nuevoAccessToken = jwtService.generarToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTipo().name()
        );

        log.info("Refresh token rotado para usuario id={}", usuario.getId());

        return ResponseEntity.ok(Map.of(
                "token",        nuevoAccessToken,
                "refreshToken", nuevoRefreshToken,
                "tipo",         usuario.getTipo().name(),
                "nombre",       usuario.getNombre(),
                "email",        usuario.getEmail(),
                "id",           usuario.getId()
        ));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            // Si no mandan refreshToken igual respondemos 200 — el cliente ya limpió su storage
            return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada."));
        }

        usuarioRepository.findByRefreshToken(refreshToken).ifPresent(usuario -> {
            usuario.setRefreshToken(null);
            usuario.setRefreshTokenExpiry(null);
            usuarioRepository.save(usuario);
            log.info("Logout: refresh token invalidado para usuario id={}", usuario.getId());
        });

        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
    }
}