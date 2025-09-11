package com.ridesync.service;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.exception.InvalidLocationDataException;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.*;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class ValidationService {
    
    private final Validator validator;
    
    public void validateLocationUpdate(LocationUpdateDto locationUpdate) {
        Set<ConstraintViolation<LocationUpdateDto>> violations = validator.validate(locationUpdate);
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder("Invalid location data: ");
            for (ConstraintViolation<LocationUpdateDto> violation : violations) {
                errors.append(violation.getPropertyPath())
                      .append(": ")
                      .append(violation.getMessage())
                      .append("; ");
            }
            throw new InvalidLocationDataException(errors.toString());
        }
    }
}