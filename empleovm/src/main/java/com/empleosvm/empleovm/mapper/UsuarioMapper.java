package com.empleosvm.empleovm.mapper;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.model.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    // ─── RequestDTO → Entidad ─────────────────────────────────────────────────
    public Usuario toEntity(UsuarioRequestDTO dto) {
        if (dto == null) return null;

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre().trim());
        usuario.setEmail(dto.getEmail().trim().toLowerCase());
        usuario.setPassword(dto.getPassword());

        if (dto.getTipo() != null && !dto.getTipo().isBlank()) {
            try {
                usuario.setTipo(Usuario.Rol.valueOf(dto.getTipo().toUpperCase()));
            } catch (IllegalArgumentException e) {
                usuario.setTipo(Usuario.Rol.ROLE_USER);
            }
        } else {
            usuario.setTipo(Usuario.Rol.ROLE_USER);
        }

        return usuario;
    }

    // ─── Entidad → ResponseDTO (incluye perfil) ───────────────────────────────
    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        if (usuario == null) return null;

        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setTipo(usuario.getTipo() != null ? usuario.getTipo().name() : "ROLE_USER");
        dto.setFechaRegistro(usuario.getFechaRegistro());

        // Campos de perfil
        dto.setFotoPerfil(usuario.getFotoPerfil());
        dto.setDescripcion(usuario.getDescripcion());
        dto.setExperiencia(usuario.getExperiencia());
        dto.setTelefono(usuario.getTelefono());
        dto.setLocalidad(usuario.getLocalidad());
        dto.setEstadoSolicitud(usuario.getEstadoSolicitud());

        return dto;
    }
}