package com.acme.employeesalary.controller;

import com.acme.employeesalary.dto.EmployeeRequestDto;
import com.acme.employeesalary.dto.EmployeeResponseDto;
import com.acme.employeesalary.dto.EmployeeUpdateRequestDto;
import com.acme.employeesalary.dto.SalaryHistoryResponseDto;
import com.acme.employeesalary.dto.SalaryUpdateRequestDto;
import com.acme.employeesalary.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeResponseDto>> getEmployees(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<EmployeeResponseDto> employees = employeeService.getEmployees(
                country, department, minSalary, maxSalary, name, pageable
        );
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
        EmployeeResponseDto employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/{id}/salary-history")
    public ResponseEntity<List<SalaryHistoryResponseDto>> getSalaryHistory(@PathVariable Long id) {
        List<SalaryHistoryResponseDto> history = employeeService.getSalaryHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeRequestDto requestDto) {
        EmployeeResponseDto created = employeeService.createEmployee(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequestDto updateDto
    ) {
        EmployeeResponseDto updated = employeeService.updateEmployee(id, updateDto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/salary")
    public ResponseEntity<EmployeeResponseDto> updateSalary(
            @PathVariable Long id,
            @Valid @RequestBody SalaryUpdateRequestDto salaryDto
    ) {
        EmployeeResponseDto updated = employeeService.updateSalary(id, salaryDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
