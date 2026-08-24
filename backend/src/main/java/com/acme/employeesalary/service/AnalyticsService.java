package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryByCountryDto;
import com.acme.employeesalary.dto.SalaryByDepartmentDto;
import com.acme.employeesalary.dto.TotalPayrollDto;

import java.util.List;

public interface AnalyticsService {

    List<SalaryByCountryDto> getSalaryByCountry();

    List<SalaryByDepartmentDto> getSalaryByDepartment();

    List<HeadcountByCountryDto> getHeadcountByCountry();

    TotalPayrollDto getTotalPayroll();
}
