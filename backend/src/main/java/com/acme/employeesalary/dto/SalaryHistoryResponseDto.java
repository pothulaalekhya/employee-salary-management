package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryHistoryResponseDto {
    private Long id;
    private Long employeeId;
    private BigDecimal amount;
    private String currency;
    private LocalDate effectiveDate;
    private Instant changedAt;
    private String note;
}
