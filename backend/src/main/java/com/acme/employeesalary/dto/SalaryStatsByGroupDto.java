package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for salary aggregate statistics (avg, median, min, max)
 * grouped by a dimension (country or department), with values converted
 * to the base reporting currency (USD).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStatsByGroupDto {
    private String group;
    private long count;
    private BigDecimal avgSalaryUsd;
    private BigDecimal medianSalaryUsd;
    private BigDecimal minSalaryUsd;
    private BigDecimal maxSalaryUsd;
}
