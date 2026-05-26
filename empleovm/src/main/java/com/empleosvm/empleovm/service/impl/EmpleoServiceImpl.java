package com.empleosvm.empleovm.service.impl;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import com.empleosvm.empleovm.mapper.EmpleoMapper;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.service.interfaces.IEmpleoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmpleoServiceImpl implements IEmpleoService {

    private final EmpleoRepository empleoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleoMapper empleoMapper;

    public EmpleoServiceImpl(EmpleoRepository empleoRepository,
                              UsuarioRepository usuarioRepository,
                              EmpleoMapper empleoMapper) {
        this.empleoRepository = empleoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empleoMapper = empleoMapper;
    }

    @Override
    @Transactional
    public EmpleoResponseDTO publicarEmpleo(EmpleoRequestDTO dto) {
        if (dto.getIdUsuario() == null) {
            throw new RuntimeException("El ID del usuario es obligatorio para publicar un empleo.");
        }

        Usuario empresa = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "No existe una empresa con ID: " + dto.getIdUsuario()));

        // Verificar que sea una cuenta de empresa
        if (empresa.getTipo() == null ||
                !empresa.getTipo().name().equals("ROLE_EMPRESA")) {
            throw new RuntimeException("Solo las cuentas de empresa pueden publicar vacantes.");
        }

        Empleo empleo = new Empleo();
        empleo.setTitulo(dto.getTitulo().trim());
        empleo.setDescripcion(dto.getDescripcion().trim());
        empleo.setEmpresa(dto.getEmpresa() != null ? dto.getEmpresa().trim() : empresa.getNombre());
        empleo.setUbicacion(dto.getUbicacion() != null ? dto.getUbicacion().trim() : "");
        empleo.setSueldo(dto.getSueldo());
        empleo.setImagenUrl(dto.getImagenUrl());
        empleo.setUsuario(empresa);
        empleo.setActivo(true);

        return empleoMapper.toResponseDTO(empleoRepository.save(empleo));
    }

    // ─── Listar todos ─────────────────────────────────────────────────────────
    @Override
    public List<EmpleoResponseDTO> listarTodos() {
        return empleoRepository.findAll()
                .stream()
                .map(empleoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Buscar por ID ────────────────────────────────────────────────────────
    @Override
    public EmpleoResponseDTO buscarPorId(Long id) {
        if (id == null) throw new RuntimeException("El ID no puede ser nulo.");
        return empleoMapper.toResponseDTO(
                empleoRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Empleo no encontrado con ID: " + id))
        );
    }

    // ─── Buscar por título ────────────────────────────────────────────────────
    @Override
    public List<EmpleoResponseDTO> buscarPorTitulo(String titulo) {
        return empleoRepository.findByTituloContainingIgnoreCase(titulo)
                .stream()
                .map(empleoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Eliminar ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!empleoRepository.existsById(id)) {
            throw new RuntimeException("No existe un empleo con ID: " + id);
        }
        empleoRepository.deleteByIdManual(id);
    }
}