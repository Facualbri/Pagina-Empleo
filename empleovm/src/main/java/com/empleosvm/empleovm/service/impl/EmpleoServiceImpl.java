package com.empleosvm.empleovm.service.impl;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import com.empleosvm.empleovm.mapper.EmpleoMapper;
import com.empleosvm.empleovm.model.entity.Empleo;
import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.EmpleoRepository;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import com.empleosvm.empleovm.service.interfaces.IEmpleoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmpleoServiceImpl implements IEmpleoService {

    @Autowired
    private EmpleoRepository empleoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleoMapper empleoMapper;

    @Override
    public EmpleoResponseDTO publicarEmpleo(EmpleoRequestDTO dto) {

        // 🔥 VALIDACIÓN IMPORTANTE
        if (dto.getIdUsuario() == null) {
            throw new RuntimeException("Error: El ID del usuario es obligatorio.");
        }

        System.out.println("DEBUG -> ID USUARIO: " + dto.getIdUsuario());

        // 🔍 Buscar usuario (empresa)
        Usuario empresa = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(
                        () -> new RuntimeException("Error: La empresa con ID " + dto.getIdUsuario() + " no existe."));

        // 🧱 Crear empleo
        Empleo empleo = new Empleo();
        empleo.setTitulo(dto.getTitulo());
        empleo.setDescripcion(dto.getDescripcion());
        empleo.setEmpresa(dto.getEmpresa());
        empleo.setUbicacion(dto.getUbicacion());
        empleo.setSueldo(dto.getSueldo());

        // 📸 IMAGEN (IMPORTANTE SI ESTÁS SUBIENDO ARCHIVOS)
        empleo.setImagenUrl(dto.getImagenUrl()); // <- asegurate que el DTO lo tenga

        // 🔗 Relación
        empleo.setUsuario(empresa);

        // 💾 Guardar
        Empleo guardado = empleoRepository.save(empleo);

        return empleoMapper.toResponseDTO(guardado);
    }

    @Override
    public List<EmpleoResponseDTO> listarTodos() {
        return empleoRepository.findAll()
                .stream()
                .map(empleoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmpleoResponseDTO buscarPorId(Long id) {
        if (id == null) {
            throw new RuntimeException("El ID del empleo no puede ser null");
        }

        Empleo empleo = empleoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleo no encontrado"));

        return empleoMapper.toResponseDTO(empleo);
    }

    @Override
    public List<EmpleoResponseDTO> buscarPorTitulo(String titulo) {
        System.out.println("DEBUG: Buscando -> " + titulo);

        return empleoRepository.findByTituloContainingIgnoreCase(titulo)
                .stream()
                .map(empleoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        // Usamos el método manual que creamos para asegurar el borrado
        empleoRepository.deleteByIdManual(id);
    }
}