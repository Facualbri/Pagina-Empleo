package com.empleosvm.empleovm.mapper;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.model.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    // De DTO a Entidad (para guardar en la base de datos)
    public Usuario toEntity(UsuarioRequestDTO dto) {
    Usuario usuario = new Usuario();
    usuario.setNombre(dto.getNombre());
    usuario.setEmail(dto.getEmail());
    usuario.setPassword(dto.getPassword());
    
    // Si el tipo no es nulo, lo convierte. Si es nulo, evita el error.
    if (dto.getTipo() != null) {
        usuario.setTipo(Usuario.Rol.valueOf(dto.getTipo().toUpperCase()));
    }
    
    return usuario;
}

    // De Entidad a DTO (para responderle al usuario sin mostrar la contraseña)
    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setTipo(usuario.getTipo().toString());
        return dto;
    }

    
}