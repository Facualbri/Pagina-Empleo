package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.model.entity.Favorito;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.repository.FavoritoRepository;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleoRepository empleoRepository;

    public FavoritoController(FavoritoRepository favoritoRepository,
                               UsuarioRepository usuarioRepository,
                               EmpleoRepository empleoRepository) {
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empleoRepository = empleoRepository;
    }

    private void verificarPropietario(Authentication auth, Long idUsuario) {
        Long authUserId = (Long) auth.getDetails();
        if (!authUserId.equals(idUsuario)) {
            throw new SecurityException("No podés acceder a favoritos de otro usuario.");
        }
    }

    // ─── Listar favoritos de un usuario ──────────────────────────────────────
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> listarFavoritos(Authentication auth, @PathVariable Long idUsuario) {
        verificarPropietario(auth, idUsuario);
        List<Favorito> favoritos = favoritoRepository.findByUsuarioId(idUsuario);

        List<Map<String, Object>> resultado = favoritos.stream().map(f -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", f.getId());
            item.put("fechaGuardado", f.getFechaGuardado());

            Map<String, Object> empleo = new HashMap<>();
            empleo.put("id", f.getEmpleo().getId());
            empleo.put("titulo", f.getEmpleo().getTitulo());
            empleo.put("empresa", f.getEmpleo().getEmpresa());
            empleo.put("ubicacion", f.getEmpleo().getUbicacion());
            empleo.put("sueldo", f.getEmpleo().getSueldo());
            empleo.put("imagenUrl", f.getEmpleo().getImagenUrl());
            empleo.put("activo", f.getEmpleo().isActivo());

            item.put("empleo", empleo);
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    // ─── Verificar si un empleo es favorito ───────────────────────────────────
    @GetMapping("/existe")
    public ResponseEntity<?> esFavorito(
            Authentication auth,
            @RequestParam Long idUsuario,
            @RequestParam Long idEmpleo) {
        verificarPropietario(auth, idUsuario);
        boolean existe = favoritoRepository.existsByUsuarioIdAndEmpleoId(idUsuario, idEmpleo);
        return ResponseEntity.ok(Map.of("esFavorito", existe));
    }

    // ─── Guardar favorito ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> guardar(
            Authentication auth,
            @RequestParam Long idUsuario,
            @RequestParam Long idEmpleo) {

        verificarPropietario(auth, idUsuario);

        if (favoritoRepository.existsByUsuarioIdAndEmpleoId(idUsuario, idEmpleo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ya está en tus favoritos."));
        }

        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        Empleo empleo = empleoRepository.findById(idEmpleo).orElse(null);

        if (usuario == null || empleo == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario o empleo no encontrado."));
        }

        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setEmpleo(empleo);
        favoritoRepository.save(favorito);

        return ResponseEntity.ok(Map.of("mensaje", "Guardado en favoritos."));
    }

    // ─── Quitar favorito ──────────────────────────────────────────────────────
    @Transactional
    @DeleteMapping
    public ResponseEntity<?> quitar(
            Authentication auth,
            @RequestParam Long idUsuario,
            @RequestParam Long idEmpleo) {

        verificarPropietario(auth, idUsuario);

        if (!favoritoRepository.existsByUsuarioIdAndEmpleoId(idUsuario, idEmpleo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No está en tus favoritos."));
        }

        favoritoRepository.deleteByUsuarioIdAndEmpleoId(idUsuario, idEmpleo);
        return ResponseEntity.ok(Map.of("mensaje", "Eliminado de favoritos."));
    }
}