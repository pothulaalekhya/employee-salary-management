package com.acme.employeesalary.controller;

import com.acme.employeesalary.dto.EmployeeRequestDto;
import com.acme.employeesalary.dto.EmployeeResponseDto;
import com.acme.employeesalary.dto.EmployeeUpdateRequestDto;
import com.acme.employeesalary.dto.SalaryHistoryResponseDto;
import com.acme.employeesalary.dto.SalaryUpdateRequestDto;
import com.acme.employeesalary.exception.DuplicateResourceException;
import com.acme.employeesalary.exception.ResourceNotFoundException;
import com.acme.employeesalary.exception.ValidationException;
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
    @DisplayName("GET /api/employees with nonsensical filter (minSalary > maxSalary) returns empty page gracefully")
    void getEmployeesWithInvertedSalaryRangeShouldReturnEmptyPageGracefully() throws Exception {
        Page<EmployeeResponseDto> emptyPage = new PageImpl<>(List.of());
        when(employeeService.getEmployees(any(), any(), eq(new BigDecimal("200000")), eq(new BigDecimal("50000")), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/employees")
                        .param("minSalary", "200000")
                        .param("maxSalary", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
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
        when(employeeService.getEmployeeById(999L)).thenThrow(new ResourceNotFoundException("Employee not found with id: 999"));

        mockMvc.perform(get("/api/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id: 999"));
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
    @DisplayName("POST /api/employees returns 400 with field-level errors on invalid input")
    void createEmployeeShouldReturn400WithFieldErrorsOnInvalidInput() throws Exception {
        EmployeeRequestDto invalidRequest = EmployeeRequestDto.builder()
                .name("") // blank
                .country("") // blank
                .department("") // blank
                .currency("INVALID") // invalid ISO code (not 3-letters)
                .initialSalary(new BigDecimal("-500.00")) // negative
                .build();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.message").value("Request validation failed on one or more fields"));
    }

    @Test
    @DisplayName("POST /api/employees returns 409 Conflict when duplicate employeeCode is supplied")
    void createEmployeeWithDuplicateCodeShouldReturn409() throws Exception {
        EmployeeRequestDto request = EmployeeRequestDto.builder()
                .employeeCode("EMP-DUPLICATE")
                .name("John Miller")
                .country("UK")
                .department("Sales")
                .currency("GBP")
                .initialSalary(new BigDecimal("75000.00"))
                .build();

        when(employeeService.createEmployee(any(EmployeeRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Employee with code 'EMP-DUPLICATE' already exists"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Employee with code 'EMP-DUPLICATE' already exists"));
    }

    @Test
    @DisplayName("POST /api/employees returns 400 Bad Request when unsupported currency is supplied")
    void createEmployeeWithUnsupportedCurrencyShouldReturn400() throws Exception {
        EmployeeRequestDto request = EmployeeRequestDto.builder()
                .name("John Miller")
                .country("UK")
                .department("Sales")
                .currency("XYZ")
                .initialSalary(new BigDecimal("75000.00"))
                .build();

        when(employeeService.createEmployee(any(EmployeeRequestDto.class)))
                .thenThrow(new ValidationException("Currency 'XYZ' is not supported. Must match an existing exchange rate row."));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Currency 'XYZ' is not supported. Must match an existing exchange rate row."));
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
    @DisplayName("PATCH /api/employees/{id}/salary returns 404 when employee does not exist")
    void updateSalaryOnNonexistentEmployeeShouldReturn404() throws Exception {
        SalaryUpdateRequestDto salaryRequest = SalaryUpdateRequestDto.builder()
                .newSalary(new BigDecimal("135000.00"))
                .effectiveDate(LocalDate.of(2024, 6, 1))
                .build();

        when(employeeService.updateSalary(eq(999L), any(SalaryUpdateRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Active employee not found with id: 999"));

        mockMvc.perform(patch("/api/employees/999/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Active employee not found with id: 999"));
    }

    @Test
    @DisplayName("PATCH /api/employees/{id}/salary returns 400 with field-level error when salary is negative or zero")
    void updateSalaryWithNegativeAmountShouldReturn400WithFieldError() throws Exception {
        SalaryUpdateRequestDto invalidRequest = SalaryUpdateRequestDto.builder()
                .newSalary(new BigDecimal("-1000.00"))
                .build();

        mockMvc.perform(patch("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("newSalary"))
                .andExpect(jsonPath("$.errors[0].message").value("New salary must be a positive amount greater than zero"));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} returns 204 No Content")
    void deleteEmployeeShouldReturn204() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());
    }
}
