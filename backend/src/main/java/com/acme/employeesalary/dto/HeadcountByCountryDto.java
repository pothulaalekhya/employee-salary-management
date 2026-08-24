package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for employee headcount grouped by country.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeadcountByCountryDto {
    private String country;
    private long count;
}
