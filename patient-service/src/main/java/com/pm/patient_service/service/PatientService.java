package com.pm.patient_service.service;

import com.pm.patient_service.dto.PatientRequestDto;
import com.pm.patient_service.dto.PatientResponseDto;
import com.pm.patient_service.exception.EmailAlreadyExistsException;
import com.pm.patient_service.exception.PatientNotFoundException;
import com.pm.patient_service.grpc.BillingServiceGrpcClient;
import com.pm.patient_service.kafka.KafkaProducer;
import com.pm.patient_service.mapper.PatientMapper;
import com.pm.patient_service.model.Patient;
import com.pm.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final  PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public List<PatientResponseDto> getPatients(){
        List<Patient> patientList =  patientRepository.findAll();
        List<PatientResponseDto> patientLitDto = patientList
                .stream()
                .map(patient -> patientMapper.toPatientResponseDto(patient))
                .toList();

        return  patientLitDto;
    }

    public PatientResponseDto createPatient(PatientRequestDto requestDto){
        Optional<Patient> patient  = patientRepository.findByEmail(requestDto.getEmail());

        patient.ifPresent(existingPatient -> {
            throw new EmailAlreadyExistsException("Patient with email '" + requestDto.getEmail() + "' already exists.");
        });

        Patient newPatient = Patient.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .address(requestDto.getAddress())
                .dateOfBirth(LocalDate.parse(requestDto.getDateOfBirth()))
                .registeredDate(LocalDate.parse(requestDto.getRegisteredDate()))
                .build();

        patientRepository.save(newPatient);

        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());

        kafkaProducer.sendMessage(newPatient);

        return  patientMapper.toPatientResponseDto(newPatient);
    }

    public PatientResponseDto updatePatient(UUID id, PatientRequestDto requestDto){
//        Patient existingPatient = patientRepository.findById(id)
//                .orElseThrow(()-> new PatientNotFoundException("Patient not found with id"+id));
//
//        Optional<Patient> patient  = patientRepository.findByEmail(requestDto.getEmail());
//
//        if(patient.isPresent()){
//            throw new EmailAlreadyExistsException("Patient already exist with email"+requestDto.getEmail());
//        }

        boolean emailExists = patientRepository.existsByEmailAndIdNot(requestDto.getEmail(), id);
        if (emailExists) {
            throw new EmailAlreadyExistsException("Patient already exists with email " + requestDto.getEmail());
        }


        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(()-> new PatientNotFoundException("Patient not found with id"+id));

        existingPatient.setName(requestDto.getName());
        existingPatient.setAddress(requestDto.getAddress());
        existingPatient.setEmail(requestDto.getEmail());
        existingPatient.setDateOfBirth(LocalDate.parse(requestDto.getDateOfBirth()));
        patientRepository.save(existingPatient);
       return patientMapper.toPatientResponseDto(existingPatient);
    }

    public void deletePatient(UUID id){
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(()-> new PatientNotFoundException("Patient not found with id"+id));
        patientRepository.deleteById(id);
    }
}
