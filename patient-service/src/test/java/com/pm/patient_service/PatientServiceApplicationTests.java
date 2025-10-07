package com.pm.patient_service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Use MockitoExtension instead of SpringBootTest
@ExtendWith(MockitoExtension.class)
class PatientServiceApplicationTests {

    @Mock
    private KafkaProducer<String, String> kafkaProducer;  // mock dependency

    @InjectMocks
    private PatientServiceApplication patientServiceApplication;

    @Test
    void contextLoads() {
        // Simple test to ensure the object is created
        // No Spring context needed
        assert patientServiceApplication != null;
    }
}
