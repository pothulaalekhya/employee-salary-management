package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.EmployeeRequestDto;
import com.acme.employeesalary.dto.EmployeeResponseDto;
import com.acme.employeesalary.dto.EmployeeUpdateRequestDto;
import com.acme.employeesalary.dto.SalaryHistoryResponseDto;
import com.acme.employeesalary.dto.SalaryUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface EmployeeService {

    Page<EmployeeResponseDto> getEmployees(
            String country,
            String department,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            String name,
            Pageable pageable
    );

    EmployeeResponseDto getEmployeeById(Long id);

    List<SalaryHistoryResponseDto> getSalaryHistory(Long employeeId);

    EmployeeResponseDto createEmployee(EmployeeRequestDto requestDto);

    EmployeeResponseDto updateEmployee(Long id, EmployeeUpdateRequestDto updateDto);

    EmployeeResponseDto updateSalary(Long id, SalaryUpdateRequestDto salaryDto);

    void deleteEmployee(Long id);
}
