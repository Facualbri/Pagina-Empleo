package com.empleosvm.empleovm.service.interfaces;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import java.util.List;

public interface IEmpleoService {
    EmpleoResponseDTO publicarEmpleo(EmpleoRequestDTO empleoDTO);
    List<EmpleoResponseDTO> listarTodos();

    List<EmpleoResponseDTO> buscarPorTitulo(String titulo);
    EmpleoResponseDTO buscarPorId(Long id);
    void eliminar(Long id);
}
