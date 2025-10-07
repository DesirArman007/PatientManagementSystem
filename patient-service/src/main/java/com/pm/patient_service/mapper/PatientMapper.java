package com.pm.patient_service.mapper;

import com.pm.patient_service.dto.PatientRequestDto;
import com.pm.patient_service.dto.PatientResponseDto;
import com.pm.patient_service.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {

    default PatientResponseDto toPatientResponseDto(Patient patient) {
        if (patient == null) return null;

        return PatientResponseDto.builder()
                .id(patient.getId().toString())
                .name(patient.getName())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .dateOfBirth(patient.getDateOfBirth().toString())
                .build();
    }

    // Convert Patient entity to PatientResponseDto
    public static Patient toModel(PatientRequestDto dto) {
        if (dto == null) return null;

        return Patient.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .dateOfBirth(LocalDate.parse(dto.getDateOfBirth()))
                .registeredDate(LocalDate.parse(dto.getRegisteredDate()))
                .build();
    }
}
