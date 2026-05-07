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
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Override
    public UsuarioResponseDTO guardarUsuario(UsuarioRequestDTO usuarioDTO) {
        try {
            Usuario usuario = usuarioMapper.toEntity(usuarioDTO);
            // Agregá este print para ver qué llega al servidor
            System.out.println("Datos recibidos: " + usuarioDTO.getNombre());

            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            return usuarioMapper.toResponseDTO(usuarioGuardado);
        } catch (Exception e) {
            // Esto te mostrará el error exacto (ej: Email duplicado) en la terminal
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO autenticar(String email, String password) {
        // Buscamos al usuario por email
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email) && u.getPassword().equals(password))
                .findFirst()
                .map(usuarioMapper::toResponseDTO)
                .orElse(null);
    }
}