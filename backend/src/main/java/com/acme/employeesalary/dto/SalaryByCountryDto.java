package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryByCountryDto {
    private String country;
    private String currency;
    private long employeeCount;
    private BigDecimal avgSalaryUsd;
    private BigDecimal medianSalaryUsd;
    private BigDecimal minSalaryUsd;
    private BigDecimal maxSalaryUsd;
}
