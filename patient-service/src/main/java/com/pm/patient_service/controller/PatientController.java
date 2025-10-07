package com.pm.patient_service.controller;

import com.pm.patient_service.dto.PatientRequestDto;
import com.pm.patient_service.dto.PatientResponseDto;
import com.pm.patient_service.dto.validators.CreatePatientValidationGroup;
import com.pm.patient_service.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/patients")
@Tag(name = "Patients", description = "API for managing Patients")
public class PatientController {

    private final  PatientService patientService;

    @GetMapping
    @Operation(summary = "Get Patients")
    public ResponseEntity<List<PatientResponseDto>> getPatients(){
        List<PatientResponseDto> patientResponseDtoList = patientService.getPatients();
        return new ResponseEntity<>(patientResponseDtoList, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create a new Patient")
    public  ResponseEntity<PatientResponseDto> registerPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class})
            @RequestBody PatientRequestDto patientRequestDto){
       PatientResponseDto createdPatient = patientService.createPatient(patientRequestDto);
        return new ResponseEntity<>(createdPatient, HttpStatus.OK);
    }

    @PutMapping(path = "/{id}")
    @Operation(summary = "Update a patient")
    public ResponseEntity<PatientResponseDto> updatePatient(  @PathVariable UUID id, @Validated({Default.class}) @RequestBody PatientRequestDto patientRequestDto){
        PatientResponseDto updatedPatient = patientService.updatePatient(id , patientRequestDto);
        return new ResponseEntity<>(updatedPatient, HttpStatus.OK);

    }

    @DeleteMapping(path = "/{id}")
    @Operation(summary = "Delete a Patient")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id){
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
