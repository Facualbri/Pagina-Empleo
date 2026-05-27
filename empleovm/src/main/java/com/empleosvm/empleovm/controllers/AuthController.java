package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.security.JwtService;
import com.empleosvm.empleovm.service.impl.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

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

    // ─── Olvidé mi contraseña ──────────────────────────────────────────────────

    @PostMapping("/olvide-password")
    public ResponseEntity<?> olvidePassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || !email.contains("@")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ingresá un email válido."));
        }

        try {
            passwordResetService.generarTokenYEnviarEmail(email.trim().toLowerCase());
            // Siempre respondemos éxito para no revelar si el email existe o no
            return ResponseEntity.ok(Map.of("mensaje",
                    "Si el email está registrado, vas a recibir un link de recuperación."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Validar token de reset ────────────────────────────────────────────────

    @GetMapping("/validar-token")
    public ResponseEntity<?> validarToken(@RequestParam String token) {
        try {
            passwordResetService.validarToken(token);
            return ResponseEntity.ok(Map.of("valido", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Restablecer contraseña ────────────────────────────────────────────────

    @PostMapping("/restablecer-password")
    public ResponseEntity<?> restablecerPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token inválido."));
        }

        try {
            passwordResetService.restablecerPassword(token, password);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente. Ya podés iniciar sesión."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}