package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for total payroll analytics: org-wide total in base currency (USD)
 * plus a per-country breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalPayrollDto {
    private BigDecimal totalPayrollUsd;
    private long totalEmployees;
    private String baseCurrency;
    private List<CountryPayroll> byCountry;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryPayroll {
        private String country;
        private long count;
        private BigDecimal payrollUsd;
    }
}
