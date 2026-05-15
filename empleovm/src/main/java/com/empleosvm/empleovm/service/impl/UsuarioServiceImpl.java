package com.empleosvm.empleovm.service.impl;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.mapper.UsuarioMapper;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.service.interfaces.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioMapper usuarioMapper;

    // ─── Guardar nuevo usuario ────────────────────────────────────────────────
    @Override
    public UsuarioResponseDTO guardarUsuario(UsuarioRequestDTO usuarioDTO) {
        // Verificar email duplicado con mensaje claro
        if (usuarioRepository.findByEmail(usuarioDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe una cuenta registrada con el email: " + usuarioDTO.getEmail());
        }
        Usuario usuario = usuarioMapper.toEntity(usuarioDTO);
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

    // ─── Autenticar ───────────────────────────────────────────────────────────
    @Override
    public UsuarioResponseDTO autenticar(String email, String password) {
        return usuarioRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password))
                .map(usuarioMapper::toResponseDTO)
                .orElse(null);
    }

    // ─── Actualizar perfil (NUEVO) ────────────────────────────────────────────
    @Override
    public UsuarioResponseDTO actualizarPerfil(Long id, Map<String, String> cambios) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        if (cambios.containsKey("nombre") && !cambios.get("nombre").isBlank()) {
            usuario.setNombre(cambios.get("nombre").trim());
        }
        if (cambios.containsKey("email") && cambios.get("email").contains("@")) {
            // Verificar que el nuevo email no esté en uso por otro usuario
            usuarioRepository.findByEmail(cambios.get("email"))
                    .ifPresent(otro -> {
                        if (!otro.getId().equals(id))
                            throw new RuntimeException("El email ya está en uso por otra cuenta.");
                    });
            usuario.setEmail(cambios.get("email").trim());
        }

        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    // ─── Cambiar contraseña (NUEVO) ───────────────────────────────────────────
    @Override
    public void cambiarPassword(Long id, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!usuario.getPassword().equals(passwordActual)) {
            throw new RuntimeException("La contraseña actual no es correcta.");
        }
        if (passwordNueva.length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        usuario.setPassword(passwordNueva);
        usuarioRepository.save(usuario);
    }
}