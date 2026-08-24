package com.acme.employeesalary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Department is required")
    private String department;

    private String title;

    /**
     * If provided in a PUT request, the server will reject it with a 400 error
     * instructing the user to use the PATCH /api/employees/{id}/salary endpoint.
     */
    private BigDecimal currentSalary;
}
