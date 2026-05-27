package com.empleosvm.empleovm.service.impl;

import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final long TOKEN_EXPIRATION_HOURS = 1;

    @Transactional
    public void generarTokenYEnviarEmail(String email) {
        String emailNorm = email.trim().toLowerCase();

        Optional<Usuario> opt = usuarioRepository.findByEmail(emailNorm);
        if (opt.isEmpty()) {
            log.info("Solicitud de reset para email no registrado: {}", emailNorm);
            return;
        }

        Usuario usuario = opt.get();
        String token = UUID.randomUUID().toString();

        usuario.setResetToken(token);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS));
        usuarioRepository.save(usuario);

        String resetLink = baseUrl + "/restablecer-password.html?token=" + token;

        try {
            enviarEmail(emailNorm, resetLink);
            log.info("Email de recuperación enviado a: {}", emailNorm);
        } catch (MessagingException e) {
            log.error("Error al enviar email de recuperación a {}: {}", emailNorm, e.getMessage());
            usuario.setResetToken(null);
            usuario.setResetTokenExpiry(null);
            usuarioRepository.save(usuario);
            throw new RuntimeException("Error al enviar el email. Intentalo de nuevo más tarde.");
        }
    }

    private void enviarEmail(String to, String resetLink) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Recuperación de contraseña - Villa María Empleos");

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Recuperar contraseña</title></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 40px;">
            <table align="center" cellpadding="0" cellspacing="0" style="max-width:520px; width:100%%; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,.08);">
              <tr>
                <td style="padding:36px 32px 8px; text-align:center;">
                  <h1 style="font-size:22px; color:#1c1c2e; margin:0 0 4px;">Villa María <span style="color:#5b5ef4;">Empleos</span></h1>
                  <p style="color:#6b7280; font-size:14px; margin:0 0 24px;">Recuperación de contraseña</p>
                </td>
              </tr>
              <tr>
                <td style="padding:0 32px 24px;">
                  <p style="color:#374151; font-size:15px; line-height:1.6; margin:0 0 20px;">
                    Hacé clic en el siguiente botón para restablecer tu contraseña. Este enlace expira en 1 hora.
                  </p>
                  <table align="center" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="background:#5b5ef4; border-radius:10px; text-align:center;">
                        <a href="%s"
                           style="display:inline-block; padding:14px 32px; color:#ffffff; font-size:15px; font-weight:700; text-decoration:none;">
                          Restablecer contraseña
                        </a>
                      </td>
                    </tr>
                  </table>
                  <p style="color:#9ca3af; font-size:12px; margin:24px 0 0; text-align:center;">
                    Si no solicitaste este cambio, podés ignorar este mensaje.
                  </p>
                </td>
              </tr>
            </table>
            </body>
            </html>
            """.formatted(resetLink);

        helper.setText(html, true);
    }

    public void validarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Token inválido.");
        }

        Usuario usuario = usuarioRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o ya fue usado."));

        if (usuario.getResetTokenExpiry() == null
                || usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            usuario.setResetToken(null);
            usuario.setResetTokenExpiry(null);
            usuarioRepository.save(usuario);
            throw new RuntimeException("El token ha expirado. Solicitá uno nuevo.");
        }
    }

    @Transactional
    public void restablecerPassword(String token, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres.");
        }

        Usuario usuario = usuarioRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido."));

        if (usuario.getResetTokenExpiry() == null
                || usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            usuario.setResetToken(null);
            usuario.setResetTokenExpiry(null);
            usuarioRepository.save(usuario);
            throw new RuntimeException("El token ha expirado. Solicitá uno nuevo.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        usuarioRepository.save(usuario);

        log.info("Contraseña restablecida para usuario ID: {}", usuario.getId());
    }
}
