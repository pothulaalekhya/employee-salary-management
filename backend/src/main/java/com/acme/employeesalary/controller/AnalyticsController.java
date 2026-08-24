package com.acme.employeesalary.controller;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryByCountryDto;
import com.acme.employeesalary.dto.SalaryByDepartmentDto;
import com.acme.employeesalary.dto.TotalPayrollDto;
import com.acme.employeesalary.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/salary-by-country")
    public ResponseEntity<List<SalaryByCountryDto>> getSalaryByCountry() {
        return ResponseEntity.ok(analyticsService.getSalaryByCountry());
    }

    @GetMapping("/salary-by-department")
    public ResponseEntity<List<SalaryByDepartmentDto>> getSalaryByDepartment() {
        return ResponseEntity.ok(analyticsService.getSalaryByDepartment());
    }

    @GetMapping("/headcount-by-country")
    public ResponseEntity<List<HeadcountByCountryDto>> getHeadcountByCountry() {
        return ResponseEntity.ok(analyticsService.getHeadcountByCountry());
    }

    @GetMapping("/total-payroll")
    public ResponseEntity<TotalPayrollDto> getTotalPayroll() {
        return ResponseEntity.ok(analyticsService.getTotalPayroll());
    }
}
