package com.empleosvm.empleovm.mapper;

import com.empleosvm.empleovm.dto.request.EmpleoRequestDTO;
import com.empleosvm.empleovm.dto.response.EmpleoResponseDTO;
import com.empleosvm.empleovm.model.entity.Empleo;
import org.springframework.stereotype.Component;

@Component
public class EmpleoMapper {

    // ─── Entidad → ResponseDTO ────────────────────────────────────────────────
    public EmpleoResponseDTO toResponseDTO(Empleo empleo) {
        if (empleo == null) return null;

        EmpleoResponseDTO dto = new EmpleoResponseDTO();
        dto.setId(empleo.getId());
        dto.setTitulo(empleo.getTitulo());
        dto.setDescripcion(empleo.getDescripcion());
        dto.setEmpresa(empleo.getEmpresa());
        dto.setUbicacion(empleo.getUbicacion());
        dto.setImagenUrl(empleo.getImagenUrl());
        dto.setSueldo(empleo.getSueldo());
        dto.setActivo(empleo.isActivo());

        if (empleo.getUsuario() != null) {
            dto.setIdUsuario(empleo.getUsuario().getId());
        }

        dto.setCantidadPostulantes(
            empleo.getPostulaciones() != null ? empleo.getPostulaciones().size() : 0
        );

        dto.setFechaPublicacion(empleo.getFechaPublicacion());

        return dto;
    }

    // ─── RequestDTO → Entidad ─────────────────────────────────────────────────
    public Empleo toEntity(EmpleoRequestDTO dto) {
        if (dto == null) return null;

        Empleo empleo = new Empleo();
        empleo.setTitulo(dto.getTitulo());
        empleo.setDescripcion(dto.getDescripcion());
        empleo.setEmpresa(dto.getEmpresa());
        empleo.setUbicacion(dto.getUbicacion());
        empleo.setSueldo(dto.getSueldo());
        empleo.setImagenUrl(dto.getImagenUrl());
        empleo.setActivo(true);
        return empleo;
    }
}