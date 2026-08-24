package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDto {
    private Long id;
    private String employeeCode;
    private String name;
    private String country;
    private String department;
    private String title;
    private String currency;
    private BigDecimal currentSalary;
    private boolean active;
    private Instant createdAt;
}
