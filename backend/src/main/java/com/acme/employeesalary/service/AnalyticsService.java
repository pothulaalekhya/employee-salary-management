package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryStatsByGroupDto;
import com.acme.employeesalary.dto.TotalPayrollDto;

import java.util.List;

/**
 * Service interface for pay analytics aggregations.
 * All data is aggregated at the SQL level with currency conversion
 * to base reporting currency (USD) at query time.
 */
public interface AnalyticsService {

    List<SalaryStatsByGroupDto> getSalaryStatsByCountry();

    List<SalaryStatsByGroupDto> getSalaryStatsByDepartment();

    List<HeadcountByCountryDto> getHeadcountByCountry();

    TotalPayrollDto getTotalPayroll();
}
