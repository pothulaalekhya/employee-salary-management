package com.acme.employeesalary.controller;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryByCountryDto;
import com.acme.employeesalary.dto.SalaryByDepartmentDto;
import com.acme.employeesalary.dto.TotalPayrollDto;
import com.acme.employeesalary.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("GET /api/analytics/salary-by-country returns 200 OK with country stats")
    void getSalaryByCountryShouldReturnList() throws Exception {
        SalaryByCountryDto dto = SalaryByCountryDto.builder()
                .country("US")
                .currency("USD")
                .employeeCount(100)
                .avgSalaryUsd(new BigDecimal("115000.00"))
                .medianSalaryUsd(new BigDecimal("112000.00"))
                .minSalaryUsd(new BigDecimal("70000.00"))
                .maxSalaryUsd(new BigDecimal("180000.00"))
                .build();

        when(analyticsService.getSalaryByCountry()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/analytics/salary-by-country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("US"))
                .andExpect(jsonPath("$[0].avgSalaryUsd").value(115000.00));
    }

    @Test
    @DisplayName("GET /api/analytics/salary-by-department returns 200 OK with department stats")
    void getSalaryByDepartmentShouldReturnList() throws Exception {
        SalaryByDepartmentDto dto = SalaryByDepartmentDto.builder()
                .department("Engineering")
                .employeeCount(150)
                .avgSalaryUsd(new BigDecimal("125000.00"))
                .medianSalaryUsd(new BigDecimal("120000.00"))
                .minSalaryUsd(new BigDecimal("80000.00"))
                .maxSalaryUsd(new BigDecimal("190000.00"))
                .build();

        when(analyticsService.getSalaryByDepartment()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/analytics/salary-by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("Engineering"))
                .andExpect(jsonPath("$[0].medianSalaryUsd").value(120000.00));
    }

    @Test
    @DisplayName("GET /api/analytics/headcount-by-country returns 200 OK with distribution")
    void getHeadcountByCountryShouldReturnList() throws Exception {
        HeadcountByCountryDto dto = HeadcountByCountryDto.builder()
                .country("India")
                .headcount(1250)
                .percentage(12.5)
                .build();

        when(analyticsService.getHeadcountByCountry()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/analytics/headcount-by-country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("India"))
                .andExpect(jsonPath("$[0].headcount").value(1250))
                .andExpect(jsonPath("$[0].percentage").value(12.5));
    }

    @Test
    @DisplayName("GET /api/analytics/total-payroll returns 200 OK with total and country breakdown")
    void getTotalPayrollShouldReturnDto() throws Exception {
        TotalPayrollDto dto = TotalPayrollDto.builder()
                .totalPayrollUsd(new BigDecimal("850000000.00"))
                .totalEmployees(10000)
                .countryBreakdown(List.of(
                        TotalPayrollDto.CountryPayrollDto.builder()
                                .country("US")
                                .totalPayrollUsd(new BigDecimal("150000000.00"))
                                .employeeCount(1300)
                                .build()
                ))
                .build();

        when(analyticsService.getTotalPayroll()).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/total-payroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayrollUsd").value(850000000.00))
                .andExpect(jsonPath("$.totalEmployees").value(10000))
                .andExpect(jsonPath("$.countryBreakdown[0].country").value("US"));
    }
}
