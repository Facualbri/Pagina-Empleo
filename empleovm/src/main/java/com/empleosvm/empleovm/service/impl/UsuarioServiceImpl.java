package com.empleosvm.empleovm.service.impl;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.mapper.UsuarioMapper;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.service.interfaces.IUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder; // BCrypt

    // ─── Guardar nuevo usuario ────────────────────────────────────────────────
    @Override
    public UsuarioResponseDTO guardarUsuario(UsuarioRequestDTO dto) {
        String emailNorm = dto.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNorm)) {
            throw new RuntimeException("Ya existe una cuenta registrada con ese email.");
        }

        Usuario usuario = usuarioMapper.toEntity(dto);
        // ✅ Hashear contraseña con BCrypt antes de guardar
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        log.info("Nuevo usuario registrado: {}", emailNorm);
        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    // ─── Listar todos ─────────────────────────────────────────────────────────
    @Override
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Buscar por ID ────────────────────────────────────────────────────────
    @Override
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return usuarioMapper.toResponseDTO(usuario);
    }

    // ─── Autenticar — devuelve el usuario si las credenciales son válidas ─────
    // ✅ Usa BCrypt para comparar (passwordEncoder.matches)
    @Override
    public UsuarioResponseDTO autenticar(String email, String password) {
        log.info("Intentando autenticar: {}", email);
        var usuario = usuarioRepository.findByEmail(email.trim().toLowerCase());
        log.info("Usuario encontrado: {}", usuario.isPresent());
        return usuario
                .filter(u -> {
                    boolean matches = passwordEncoder.matches(password, u.getPassword());
                    log.info("Password matches: {}", matches);
                    return matches;
                })
                .map(usuarioMapper::toResponseDTO)
                .orElse(null);
    }

    // ─── Actualizar perfil ────────────────────────────────────────────────────
    @Override
    public UsuarioResponseDTO actualizarPerfil(Long id, Map<String, String> cambios) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        if (cambios.containsKey("nombre") && !cambios.get("nombre").isBlank()) {
            usuario.setNombre(cambios.get("nombre").trim());
        }
        if (cambios.containsKey("email")) {
            String nuevoEmail = cambios.get("email").trim().toLowerCase();
            if (!nuevoEmail.contains("@")) {
                throw new RuntimeException("El email no es válido.");
            }
            usuarioRepository.findByEmail(nuevoEmail).ifPresent(otro -> {
                if (!otro.getId().equals(id))
                    throw new RuntimeException("El email ya está en uso por otra cuenta.");
            });
            usuario.setEmail(nuevoEmail);
        }

        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    // ─── Cambiar contraseña ───────────────────────────────────────────────────
    // ✅ Verifica con BCrypt y hashea la nueva
    @Override
    public void cambiarPassword(Long id, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual no es correcta.");
        }
        if (passwordNueva.length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
        log.info("Contraseña actualizada para usuario ID: {}", id);
    }
}