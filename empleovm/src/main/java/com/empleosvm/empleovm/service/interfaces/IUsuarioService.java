package com.empleosvm.empleovm.service.interfaces;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import java.util.List;

public interface IUsuarioService {
    UsuarioResponseDTO guardarUsuario(UsuarioRequestDTO usuarioDTO);
    List<UsuarioResponseDTO> listarUsuarios();
    UsuarioResponseDTO buscarPorId(Long id);

    UsuarioResponseDTO autenticar(String email, String password);
}