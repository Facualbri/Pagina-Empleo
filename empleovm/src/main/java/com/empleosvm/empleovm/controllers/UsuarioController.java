package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.dto.request.UsuarioRequestDTO;
import com.empleosvm.empleovm.dto.response.UsuarioResponseDTO;
import com.empleosvm.empleovm.service.interfaces.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios") // Esta es la URL base
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;

    // Crear un nuevo usuario (POST)
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crearUsuario(@RequestBody UsuarioRequestDTO usuarioDTO) {
        UsuarioResponseDTO respuesta = usuarioService.guardarUsuario(usuarioDTO);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // Listar todos (GET)
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuarios() {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    // Buscar uno por ID (GET)
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioRequestDTO loginDTO) {
        UsuarioResponseDTO usuario = usuarioService.autenticar(loginDTO.getEmail(), loginDTO.getPassword());
        if (usuario != null) {
            return ResponseEntity.ok(usuario);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }
}