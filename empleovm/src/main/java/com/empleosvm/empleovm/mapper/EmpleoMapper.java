package com.empleosvm.empleovm.mapper;

import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import com.empleosvm.empleovm.model.entity.Empleo;
import org.springframework.stereotype.Component;

@Component
public class EmpleoMapper {

    /**
     * Convierte la entidad Empleo a un DTO de respuesta.
     * Este DTO es el que viaja al frontend y permite filtrar por empresa.
     */
    public EmpleoResponseDTO toResponseDTO(Empleo empleo) {
        if (empleo == null) {
            return null;
        }

        EmpleoResponseDTO dto = new EmpleoResponseDTO();
        
        // Datos básicos del empleo
        dto.setId(empleo.getId());
        dto.setTitulo(empleo.getTitulo());
        dto.setDescripcion(empleo.getDescripcion());
        dto.setEmpresa(empleo.getEmpresa());
        dto.setUbicacion(empleo.getUbicacion());
        dto.setImagenUrl(empleo.getImagenUrl());

        // 1. MAPEAMOS EL SUELDO 
        // Asegurate que en EmpleoResponseDTO el campo sea 'sueldo' (Double)
        dto.setSueldo(empleo.getSueldo()); 
        
        // 2. MAPEAMOS EL ID DEL USUARIO (VITAL PARA EL FILTRO)
        // Esto saca el ID (como el 5 o 6 que vimos en tu DB) y lo mete en el JSON
        if (empleo.getUsuario() != null) {
            dto.setIdUsuario(empleo.getUsuario().getId()); 
            
            // Opcional: si querés mostrar el nombre del dueño en algún lado
            // dto.setNombrePublicador(empleo.getUsuario().getNombre());
        }
        
        return dto;
    }
}