package com.acme.employeesalary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeadcountByCountryDto {
    private String country;
    private long headcount;
    private double percentage;
}
