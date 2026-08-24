package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalPayrollDto {
    private BigDecimal totalPayrollUsd;
    private long totalEmployees;
    private List<CountryPayrollDto> countryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryPayrollDto {
        private String country;
        private BigDecimal totalPayrollUsd;
        private long employeeCount;
    }
}
