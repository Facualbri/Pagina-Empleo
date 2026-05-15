package com.empleosvm.empleovm.service.interfaces;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;

import java.util.List;
import java.util.Map;

public interface IUsuarioService {
    UsuarioResponseDTO guardarUsuario(UsuarioRequestDTO usuarioDTO);
    List<UsuarioResponseDTO> listarUsuarios();
    UsuarioResponseDTO buscarPorId(Long id);
    UsuarioResponseDTO autenticar(String email, String password);

    // NUEVOS
    UsuarioResponseDTO actualizarPerfil(Long id, Map<String, String> cambios);
    void cambiarPassword(Long id, String passwordActual, String passwordNueva);
}