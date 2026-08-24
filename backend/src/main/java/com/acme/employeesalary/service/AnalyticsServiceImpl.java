package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryStatsByGroupDto;
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

/**
 * Analytics service implementation. All aggregation is performed at the SQL level —
 * this class only maps the native query results (Object[]) to typed DTOs.
 * No rows are pulled into Java for in-memory aggregation.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStatsByGroupDto> getSalaryStatsByCountry() {
        List<Object[]> statsRows = analyticsRepository.findSalaryStatsByCountry();
        List<Object[]> medianRows = analyticsRepository.findMedianSalaryByCountry();

        // Build a lookup map: group -> median
        Map<String, BigDecimal> medianMap = new HashMap<>();
        for (Object[] row : medianRows) {
            String group = (String) row[0];
            BigDecimal median = toBigDecimal(row[1]);
            medianMap.put(group, median);
        }

        List<SalaryStatsByGroupDto> results = new ArrayList<>();
        for (Object[] row : statsRows) {
            String group = (String) row[0];
            long count = toLong(row[1]);
            BigDecimal avg = toBigDecimal(row[2]);
            BigDecimal min = toBigDecimal(row[3]);
            BigDecimal max = toBigDecimal(row[4]);
            BigDecimal median = medianMap.getOrDefault(group, BigDecimal.ZERO);

            results.add(SalaryStatsByGroupDto.builder()
                    .group(group)
                    .count(count)
                    .avgSalaryUsd(avg.setScale(2, RoundingMode.HALF_UP))
                    .medianSalaryUsd(median.setScale(2, RoundingMode.HALF_UP))
                    .minSalaryUsd(min.setScale(2, RoundingMode.HALF_UP))
                    .maxSalaryUsd(max.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStatsByGroupDto> getSalaryStatsByDepartment() {
        List<Object[]> statsRows = analyticsRepository.findSalaryStatsByDepartment();
        List<Object[]> medianRows = analyticsRepository.findMedianSalaryByDepartment();

        Map<String, BigDecimal> medianMap = new HashMap<>();
        for (Object[] row : medianRows) {
            String group = (String) row[0];
            BigDecimal median = toBigDecimal(row[1]);
            medianMap.put(group, median);
        }

        List<SalaryStatsByGroupDto> results = new ArrayList<>();
        for (Object[] row : statsRows) {
            String group = (String) row[0];
            long count = toLong(row[1]);
            BigDecimal avg = toBigDecimal(row[2]);
            BigDecimal min = toBigDecimal(row[3]);
            BigDecimal max = toBigDecimal(row[4]);
            BigDecimal median = medianMap.getOrDefault(group, BigDecimal.ZERO);

            results.add(SalaryStatsByGroupDto.builder()
                    .group(group)
                    .count(count)
                    .avgSalaryUsd(avg.setScale(2, RoundingMode.HALF_UP))
                    .medianSalaryUsd(median.setScale(2, RoundingMode.HALF_UP))
                    .minSalaryUsd(min.setScale(2, RoundingMode.HALF_UP))
                    .maxSalaryUsd(max.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeadcountByCountryDto> getHeadcountByCountry() {
        List<Object[]> rows = analyticsRepository.findHeadcountByCountry();
        List<HeadcountByCountryDto> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(HeadcountByCountryDto.builder()
                    .country((String) row[0])
                    .count(toLong(row[1]))
                    .build());
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public TotalPayrollDto getTotalPayroll() {
        Object[] totalRow = analyticsRepository.findTotalPayroll();
        BigDecimal totalUsd = toBigDecimal(totalRow[0]);
        long totalCount = toLong(totalRow[1]);

        List<Object[]> countryRows = analyticsRepository.findPayrollByCountry();
        List<TotalPayrollDto.CountryPayroll> breakdown = new ArrayList<>();
        for (Object[] row : countryRows) {
            breakdown.add(TotalPayrollDto.CountryPayroll.builder()
                    .country((String) row[0])
                    .count(toLong(row[1]))
                    .payrollUsd(toBigDecimal(row[2]).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        return TotalPayrollDto.builder()
                .totalPayrollUsd(totalUsd.setScale(2, RoundingMode.HALF_UP))
                .totalEmployees(totalCount)
                .baseCurrency("USD")
                .byCountry(breakdown)
                .build();
    }

    // ─── Helper methods for safe type conversion from native query results ──

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        return Long.parseLong(value.toString());
    }
}
