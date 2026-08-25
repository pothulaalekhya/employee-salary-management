package com.acme.employeesalary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryUpdateRequestDto {

    @NotNull(message = "New salary is required")
    @Positive(message = "New salary must be a positive amount greater than zero")
    private BigDecimal newSalary;

    private LocalDate effectiveDate;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;
}
