package com.pm.patient_service.dto;


import com.pm.patient_service.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientRequestDto {

    @NotBlank(message = "Name cannot be empty")
    @Size(min = 3 , max = 100, message = "Name cannot should be in between 3 and 100 Characters")
    private String name;

    @NotBlank(message = "Address cannot be empty")
    private String address;

    @Email
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Date of birth cannot be empty")
    private String dateOfBirth;

    @NotBlank(groups = CreatePatientValidationGroup.class, message = "Registeration date cannot be empty")
    private String registeredDate;


}
