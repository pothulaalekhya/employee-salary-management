package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.EmployeeRequestDto;
import com.acme.employeesalary.dto.EmployeeResponseDto;
import com.acme.employeesalary.dto.EmployeeUpdateRequestDto;
import com.acme.employeesalary.dto.SalaryHistoryResponseDto;
import com.acme.employeesalary.dto.SalaryUpdateRequestDto;
import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.SalaryHistory;
import com.acme.employeesalary.exception.ResourceNotFoundException;
import com.acme.employeesalary.exception.ValidationException;
import com.acme.employeesalary.repository.EmployeeRepository;
import com.acme.employeesalary.repository.EmployeeSpecification;
import com.acme.employeesalary.repository.ExchangeRateRepository;
import com.acme.employeesalary.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDto> getEmployees(
            String country,
            String department,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            String name,
            Pageable pageable
    ) {
        Specification<Employee> spec = EmployeeSpecification.withFilters(
                country, department, minSalary, maxSalary, name
        );
        return employeeRepository.findAll(spec, pageable).map(this::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (!employee.isActive()) {
            throw new ResourceNotFoundException("Employee with id " + id + " is inactive");
        }

        return toResponseDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryHistoryResponseDto> getSalaryHistory(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (!employee.isActive()) {
            throw new ResourceNotFoundException("Employee with id " + employeeId + " is inactive");
        }

        return salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDescChangedAtDesc(employeeId)
                .stream()
                .map(this::toSalaryHistoryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Employee name is required");
        }
        if (requestDto.getCountry() == null || requestDto.getCountry().trim().isEmpty()) {
            throw new ValidationException("Employee country is required");
        }
        if (requestDto.getDepartment() == null || requestDto.getDepartment().trim().isEmpty()) {
            throw new ValidationException("Employee department is required");
        }
        if (requestDto.getCurrency() == null || requestDto.getCurrency().trim().isEmpty()) {
            throw new ValidationException("Currency is required");
        }
        if (requestDto.getInitialSalary() == null || requestDto.getInitialSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Initial salary must be a positive amount");
        }

        String currency = requestDto.getCurrency().trim().toUpperCase();
        if (!exchangeRateRepository.existsByCurrencyCode(currency)) {
            throw new ValidationException("Currency '" + currency + "' is not supported. Must match an existing exchange rate row.");
        }

        String employeeCode = requestDto.getEmployeeCode();
        if (employeeCode == null || employeeCode.trim().isEmpty()) {
            employeeCode = "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else {
            employeeCode = employeeCode.trim().toUpperCase();
            if (employeeRepository.existsByEmployeeCode(employeeCode)) {
                throw new ValidationException("Employee with code '" + employeeCode + "' already exists");
            }
        }

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .name(requestDto.getName().trim())
                .country(requestDto.getCountry().trim())
                .department(requestDto.getDepartment().trim())
                .title(requestDto.getTitle() != null ? requestDto.getTitle().trim() : null)
                .currency(currency)
                .currentSalary(requestDto.getInitialSalary())
                .active(true)
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        SalaryHistory initialHistory = SalaryHistory.builder()
                .employee(savedEmployee)
                .amount(savedEmployee.getCurrentSalary())
                .currency(savedEmployee.getCurrency())
                .effectiveDate(requestDto.getEffectiveDate() != null ? requestDto.getEffectiveDate() : LocalDate.now())
                .note(requestDto.getNote() != null ? requestDto.getNote().trim() : "Initial salary on record creation")
                .build();

        salaryHistoryRepository.save(initialHistory);

        return toResponseDto(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponseDto updateEmployee(Long id, EmployeeUpdateRequestDto updateDto) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found with id: " + id));

        if (updateDto.getCurrentSalary() != null) {
            throw new ValidationException("Salary updates are not allowed via PUT. Please use PATCH /api/employees/" + id + "/salary to update salary with history tracking.");
        }

        if (updateDto.getName() != null && !updateDto.getName().trim().isEmpty()) {
            employee.setName(updateDto.getName().trim());
        }
        if (updateDto.getCountry() != null && !updateDto.getCountry().trim().isEmpty()) {
            employee.setCountry(updateDto.getCountry().trim());
        }
        if (updateDto.getDepartment() != null && !updateDto.getDepartment().trim().isEmpty()) {
            employee.setDepartment(updateDto.getDepartment().trim());
        }
        if (updateDto.getTitle() != null) {
            employee.setTitle(updateDto.getTitle().trim());
        }

        Employee updated = employeeRepository.save(employee);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public EmployeeResponseDto updateSalary(Long id, SalaryUpdateRequestDto salaryDto) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found with id: " + id));

        if (salaryDto.getNewSalary() == null || salaryDto.getNewSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("New salary must be a positive amount");
        }

        if (salaryDto.getNewSalary().compareTo(employee.getCurrentSalary()) == 0) {
            throw new ValidationException("New salary must be different from current salary of " + employee.getCurrentSalary());
        }

        SalaryHistory newHistory = SalaryHistory.builder()
                .employee(employee)
                .amount(salaryDto.getNewSalary())
                .currency(employee.getCurrency())
                .effectiveDate(salaryDto.getEffectiveDate() != null ? salaryDto.getEffectiveDate() : LocalDate.now())
                .note(salaryDto.getNote() != null ? salaryDto.getNote().trim() : "Compensation adjustment")
                .build();

        salaryHistoryRepository.save(newHistory);

        employee.setCurrentSalary(salaryDto.getNewSalary());
        Employee updatedEmployee = employeeRepository.save(employee);

        return toResponseDto(updatedEmployee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found with id: " + id));

        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private EmployeeResponseDto toResponseDto(Employee employee) {
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .name(employee.getName())
                .country(employee.getCountry())
                .department(employee.getDepartment())
                .title(employee.getTitle())
                .currency(employee.getCurrency())
                .currentSalary(employee.getCurrentSalary())
                .active(employee.isActive())
                .createdAt(employee.getCreatedAt())
                .build();
    }

    private SalaryHistoryResponseDto toSalaryHistoryDto(SalaryHistory history) {
        return SalaryHistoryResponseDto.builder()
                .id(history.getId())
                .employeeId(history.getEmployee().getId())
                .amount(history.getAmount())
                .currency(history.getCurrency())
                .effectiveDate(history.getEffectiveDate())
                .changedAt(history.getChangedAt())
                .note(history.getNote())
                .build();
    }
}
