package com.empleosvm.empleovm.service.interfaces;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;

import java.util.List;

public interface IEmpleoService {

    // Publicar un nuevo empleo
    EmpleoResponseDTO publicarEmpleo(EmpleoRequestDTO empleoDTO);

    // Listar todos los empleos
    List<EmpleoResponseDTO> listarTodos();

    // Buscar por título (parcial, case-insensitive)
    List<EmpleoResponseDTO> buscarPorTitulo(String titulo);

    // Buscar uno por ID
    EmpleoResponseDTO buscarPorId(Long id);

    // Eliminar por ID
    void eliminar(Long id);
}