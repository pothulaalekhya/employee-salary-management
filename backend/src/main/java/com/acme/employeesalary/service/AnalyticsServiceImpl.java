package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryByCountryDto;
import com.acme.employeesalary.dto.SalaryByDepartmentDto;
import com.acme.employeesalary.dto.TotalPayrollDto;
import com.acme.employeesalary.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryByCountryDto> getSalaryByCountry() {
        List<AnalyticsRepository.SalaryAggregationProjection> stats = analyticsRepository.findSalaryByCountryStats();
        List<AnalyticsRepository.MedianProjection> medians = analyticsRepository.findMedianSalaryByCountry();

        Map<String, BigDecimal> medianMap = new HashMap<>();
        for (AnalyticsRepository.MedianProjection m : medians) {
            medianMap.put(m.getGroupKey(), m.getMedianSalary());
        }

        List<SalaryByCountryDto> result = new ArrayList<>();
        for (AnalyticsRepository.SalaryAggregationProjection s : stats) {
            result.add(SalaryByCountryDto.builder()
                    .country(s.getGroupKey())
                    .currency(s.getCurrency())
                    .employeeCount(s.getEmpCount() != null ? s.getEmpCount() : 0L)
                    .avgSalaryUsd(s.getAvgSalary() != null ? s.getAvgSalary() : BigDecimal.ZERO)
                    .medianSalaryUsd(medianMap.getOrDefault(s.getGroupKey(), s.getAvgSalary() != null ? s.getAvgSalary() : BigDecimal.ZERO))
                    .minSalaryUsd(s.getMinSalary() != null ? s.getMinSalary() : BigDecimal.ZERO)
                    .maxSalaryUsd(s.getMaxSalary() != null ? s.getMaxSalary() : BigDecimal.ZERO)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryByDepartmentDto> getSalaryByDepartment() {
        List<AnalyticsRepository.SalaryAggregationProjection> stats = analyticsRepository.findSalaryByDepartmentStats();
        List<AnalyticsRepository.MedianProjection> medians = analyticsRepository.findMedianSalaryByDepartment();

        Map<String, BigDecimal> medianMap = new HashMap<>();
        for (AnalyticsRepository.MedianProjection m : medians) {
            medianMap.put(m.getGroupKey(), m.getMedianSalary());
        }

        List<SalaryByDepartmentDto> result = new ArrayList<>();
        for (AnalyticsRepository.SalaryAggregationProjection s : stats) {
            result.add(SalaryByDepartmentDto.builder()
                    .department(s.getGroupKey())
                    .employeeCount(s.getEmpCount() != null ? s.getEmpCount() : 0L)
                    .avgSalaryUsd(s.getAvgSalary() != null ? s.getAvgSalary() : BigDecimal.ZERO)
                    .medianSalaryUsd(medianMap.getOrDefault(s.getGroupKey(), s.getAvgSalary() != null ? s.getAvgSalary() : BigDecimal.ZERO))
                    .minSalaryUsd(s.getMinSalary() != null ? s.getMinSalary() : BigDecimal.ZERO)
                    .maxSalaryUsd(s.getMaxSalary() != null ? s.getMaxSalary() : BigDecimal.ZERO)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeadcountByCountryDto> getHeadcountByCountry() {
        List<AnalyticsRepository.HeadcountProjection> list = analyticsRepository.findHeadcountByCountry();

        long totalHeadcount = list.stream()
                .mapToLong(p -> p.getHeadcount() != null ? p.getHeadcount() : 0L)
                .sum();

        List<HeadcountByCountryDto> result = new ArrayList<>();
        for (AnalyticsRepository.HeadcountProjection p : list) {
            long count = p.getHeadcount() != null ? p.getHeadcount() : 0L;
            double percentage = totalHeadcount > 0 ? (double) count / totalHeadcount * 100.0 : 0.0;
            BigDecimal roundedPercentage = BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP);

            result.add(HeadcountByCountryDto.builder()
                    .country(p.getCountry())
                    .headcount(count)
                    .percentage(roundedPercentage.doubleValue())
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TotalPayrollDto getTotalPayroll() {
        AnalyticsRepository.TotalPayrollProjection total = analyticsRepository.findTotalPayroll();
        List<AnalyticsRepository.CountryPayrollProjection> breakdown = analyticsRepository.findPayrollBreakdownByCountry();

        List<TotalPayrollDto.CountryPayrollDto> countryBreakdown = new ArrayList<>();
        for (AnalyticsRepository.CountryPayrollProjection c : breakdown) {
            countryBreakdown.add(TotalPayrollDto.CountryPayrollDto.builder()
                    .country(c.getCountry())
                    .totalPayrollUsd(c.getTotalPayroll() != null ? c.getTotalPayroll() : BigDecimal.ZERO)
                    .employeeCount(c.getEmployeeCount() != null ? c.getEmployeeCount() : 0L)
                    .build());
        }

        return TotalPayrollDto.builder()
                .totalPayrollUsd(total != null && total.getTotalPayroll() != null ? total.getTotalPayroll() : BigDecimal.ZERO)
                .totalEmployees(total != null && total.getTotalEmployees() != null ? total.getTotalEmployees() : 0L)
                .countryBreakdown(countryBreakdown)
                .build();
    }
}
