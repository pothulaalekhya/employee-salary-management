package com.acme.employeesalary.controller;

import com.acme.employeesalary.dto.EmployeeRequestDto;
import com.acme.employeesalary.dto.EmployeeResponseDto;
import com.acme.employeesalary.dto.EmployeeUpdateRequestDto;
import com.acme.employeesalary.dto.SalaryHistoryResponseDto;
import com.acme.employeesalary.dto.SalaryUpdateRequestDto;
import com.acme.employeesalary.exception.ResourceNotFoundException;
import com.acme.employeesalary.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private final EmployeeResponseDto sampleEmployee = EmployeeResponseDto.builder()
            .id(1L)
            .employeeCode("EMP-001")
            .name("Sarah Connor")
            .country("US")
            .department("Engineering")
            .title("Senior Engineer")
            .currency("USD")
            .currentSalary(new BigDecimal("120000.00"))
            .active(true)
            .createdAt(Instant.now())
            .build();

    @Test
    @DisplayName("GET /api/employees returns 200 OK with paginated list")
    void getEmployeesShouldReturnPage() throws Exception {
        Page<EmployeeResponseDto> page = new PageImpl<>(List.of(sampleEmployee));
        when(employeeService.getEmployees(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("country", "US")
                        .param("department", "Engineering")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-001"))
                .andExpect(jsonPath("$.content[0].name").value("Sarah Connor"));
    }

    @Test
    @DisplayName("GET /api/employees/{id} returns 200 OK when found")
    void getEmployeeByIdShouldReturnEmployee() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleEmployee);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sarah Connor"));
    }

    @Test
    @DisplayName("GET /api/employees/{id} returns 404 when not found")
    void getEmployeeByIdShouldReturn404() throws Exception {
        when(employeeService.getEmployeeById(999L)).thenThrow(new ResourceNotFoundException("Employee not found"));

        mockMvc.perform(get("/api/employees/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/employees/{id}/salary-history returns 200 OK with history list")
    void getSalaryHistoryShouldReturnList() throws Exception {
        SalaryHistoryResponseDto history = SalaryHistoryResponseDto.builder()
                .id(10L)
                .employeeId(1L)
                .amount(new BigDecimal("120000.00"))
                .currency("USD")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .changedAt(Instant.now())
                .note("Hiring")
                .build();

        when(employeeService.getSalaryHistory(1L)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/employees/1/salary-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(120000.00))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    @DisplayName("POST /api/employees returns 201 Created on valid body")
    void createEmployeeShouldReturn201() throws Exception {
        EmployeeRequestDto request = EmployeeRequestDto.builder()
                .name("John Miller")
                .country("UK")
                .department("Sales")
                .currency("GBP")
                .initialSalary(new BigDecimal("75000.00"))
                .build();

        when(employeeService.createEmployee(any(EmployeeRequestDto.class))).thenReturn(sampleEmployee);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/employees returns 400 Bad Request when validation fails")
    void createEmployeeShouldReturn400OnInvalidInput() throws Exception {
        EmployeeRequestDto invalidRequest = EmployeeRequestDto.builder()
                .name("") // blank name
                .country("US")
                .department("Sales")
                .currency("USD")
                .initialSalary(new BigDecimal("-10.00")) // negative
                .build();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/employees/{id} returns 200 OK on non-salary update")
    void updateEmployeeShouldReturn200() throws Exception {
        EmployeeUpdateRequestDto updateRequest = EmployeeUpdateRequestDto.builder()
                .name("Sarah Connor-Reese")
                .country("US")
                .department("Leadership")
                .title("VP of Engineering")
                .build();

        when(employeeService.updateEmployee(eq(1L), any(EmployeeUpdateRequestDto.class))).thenReturn(sampleEmployee);

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/employees/{id}/salary returns 200 OK on valid salary change")
    void updateSalaryShouldReturn200() throws Exception {
        SalaryUpdateRequestDto salaryRequest = SalaryUpdateRequestDto.builder()
                .newSalary(new BigDecimal("135000.00"))
                .effectiveDate(LocalDate.of(2024, 6, 1))
                .note("Promotion raise")
                .build();

        when(employeeService.updateSalary(eq(1L), any(SalaryUpdateRequestDto.class))).thenReturn(sampleEmployee);

        mockMvc.perform(patch("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} returns 204 No Content")
    void deleteEmployeeShouldReturn204() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());
    }
}
