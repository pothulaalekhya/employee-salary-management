package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryStatsByGroupDto;
import com.acme.employeesalary.dto.TotalPayrollDto;
import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.ExchangeRate;
import com.acme.employeesalary.repository.AnalyticsRepository;
import com.acme.employeesalary.repository.EmployeeRepository;
import com.acme.employeesalary.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service-layer integration test for analytics.
 *
 * Uses a SMALL, KNOWN, fixed dataset (6 employees across 3 currencies) with
 * hand-calculated expected values. This is the only way to actually prove the
 * currency-conversion math is correct rather than just "runs without error."
 *
 * The test runs against H2 in MySQL compatibility mode (application-test.properties).
 *
 * ── Test Dataset ─────────────────────────────────────────────────────────
 *
 * Exchange rates:
 *   USD → 1.000000  (identity)
 *   GBP → 1.270000
 *   INR → 0.012000
 *
 * Employees (all active):
 *   1. Alice   | US    | Engineering | USD | 100,000    → 100,000.00 USD
 *   2. Bob     | US    | Sales       | USD |  80,000    →  80,000.00 USD
 *   3. Charlie | UK    | Engineering | GBP |  60,000    →  76,200.00 USD
 *   4. Diana   | UK    | Sales       | GBP |  50,000    →  63,500.00 USD
 *   5. Eve     | India | Engineering | INR | 2,000,000  →  24,000.00 USD
 *   6. Frank   | India | Sales       | INR | 1,500,000  →  18,000.00 USD
 *
 * ── Hand-calculated expected results ────────────────────────────────────
 *
 * salary-by-country:
 *   India: count=2, converted=[18000,24000], avg=21000.00, min=18000.00, max=24000.00, median=21000.00
 *   UK:    count=2, converted=[63500,76200], avg=69850.00, min=63500.00, max=76200.00, median=69850.00
 *   US:    count=2, converted=[80000,100000], avg=90000.00, min=80000.00, max=100000.00, median=90000.00
 *
 * salary-by-department:
 *   Engineering: count=3, converted=[24000,76200,100000], avg=66733.33, min=24000.00, max=100000.00, median=76200.00
 *   Sales:       count=3, converted=[18000,63500,80000],  avg=53833.33, min=18000.00, max=80000.00, median=63500.00
 *
 * headcount-by-country:
 *   India=2, UK=2, US=2
 *
 * total-payroll:
 *   total = 100000 + 80000 + 76200 + 63500 + 24000 + 18000 = 361,700.00 USD
 *   by-country: India=42000.00, UK=139700.00, US=180000.00
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsServiceImplTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @BeforeEach
    void setUp() {
        // Clear any existing data (other tests, seeder, etc.)
        employeeRepository.deleteAll();
        exchangeRateRepository.deleteAll();

        // Seed exchange rates
        exchangeRateRepository.save(ExchangeRate.builder()
                .currencyCode("USD").rateToBase(new BigDecimal("1.000000")).baseCurrency("USD").build());
        exchangeRateRepository.save(ExchangeRate.builder()
                .currencyCode("GBP").rateToBase(new BigDecimal("1.270000")).baseCurrency("USD").build());
        exchangeRateRepository.save(ExchangeRate.builder()
                .currencyCode("INR").rateToBase(new BigDecimal("0.012000")).baseCurrency("USD").build());

        // Seed employees
        Instant now = Instant.now();
        employeeRepository.save(Employee.builder()
                .employeeCode("TEST-001").name("Alice").country("US").department("Engineering")
                .title("Software Engineer").currency("USD").currentSalary(new BigDecimal("100000.00"))
                .active(true).createdAt(now).build());
        employeeRepository.save(Employee.builder()
                .employeeCode("TEST-002").name("Bob").country("US").department("Sales")
                .title("Sales Rep").currency("USD").currentSalary(new BigDecimal("80000.00"))
                .active(true).createdAt(now).build());
        employeeRepository.save(Employee.builder()
                .employeeCode("TEST-003").name("Charlie").country("UK").department("Engineering")
                .title("Senior Engineer").currency("GBP").currentSalary(new BigDecimal("60000.00"))
                .active(true).createdAt(now).build());
        employeeRepository.save(Employee.builder()
                .employeeCode("TEST-004").name("Diana").country("UK").department("Sales")
                .title("Account Executive").currency("GBP").currentSalary(new BigDecimal("50000.00"))
                .active(true).createdAt(now).build());
        employeeRepository.save(Employee.builder()
                .employeeCode("TEST-005").name("Eve").country("India").department("Engineering")
                .title("Tech Lead").currency("INR").currentSalary(new BigDecimal("2000000.00"))
                .active(true).createdAt(now).build());
        employeeRepository.save(Employee.builder()
                .employeeCode("TEST-006").name("Frank").country("India").department("Sales")
                .title("Business Development").currency("INR").currentSalary(new BigDecimal("1500000.00"))
                .active(true).createdAt(now).build());
    }

    @Test
    @DisplayName("salary-by-country: avg, median, min, max with currency conversion to USD")
    void testSalaryStatsByCountry() {
        List<SalaryStatsByGroupDto> results = analyticsService.getSalaryStatsByCountry();

        assertEquals(3, results.size(), "Should have 3 countries");

        Map<String, SalaryStatsByGroupDto> byCountry = results.stream()
                .collect(Collectors.toMap(SalaryStatsByGroupDto::getGroup, r -> r));

        // India: converted values [18000, 24000]
        SalaryStatsByGroupDto india = byCountry.get("India");
        assertNotNull(india, "India should be present");
        assertEquals(2, india.getCount());
        assertBigDecimalEquals("21000.00", india.getAvgSalaryUsd(), "India avg");
        assertBigDecimalEquals("18000.00", india.getMinSalaryUsd(), "India min");
        assertBigDecimalEquals("24000.00", india.getMaxSalaryUsd(), "India max");
        assertBigDecimalEquals("21000.00", india.getMedianSalaryUsd(), "India median");

        // UK: converted values [63500, 76200]
        SalaryStatsByGroupDto uk = byCountry.get("UK");
        assertNotNull(uk, "UK should be present");
        assertEquals(2, uk.getCount());
        assertBigDecimalEquals("69850.00", uk.getAvgSalaryUsd(), "UK avg");
        assertBigDecimalEquals("63500.00", uk.getMinSalaryUsd(), "UK min");
        assertBigDecimalEquals("76200.00", uk.getMaxSalaryUsd(), "UK max");
        assertBigDecimalEquals("69850.00", uk.getMedianSalaryUsd(), "UK median");

        // US: converted values [80000, 100000]
        SalaryStatsByGroupDto us = byCountry.get("US");
        assertNotNull(us, "US should be present");
        assertEquals(2, us.getCount());
        assertBigDecimalEquals("90000.00", us.getAvgSalaryUsd(), "US avg");
        assertBigDecimalEquals("80000.00", us.getMinSalaryUsd(), "US min");
        assertBigDecimalEquals("100000.00", us.getMaxSalaryUsd(), "US max");
        assertBigDecimalEquals("90000.00", us.getMedianSalaryUsd(), "US median");
    }

    @Test
    @DisplayName("salary-by-department: avg, median, min, max with currency conversion to USD")
    void testSalaryStatsByDepartment() {
        List<SalaryStatsByGroupDto> results = analyticsService.getSalaryStatsByDepartment();

        assertEquals(2, results.size(), "Should have 2 departments");

        Map<String, SalaryStatsByGroupDto> byDept = results.stream()
                .collect(Collectors.toMap(SalaryStatsByGroupDto::getGroup, r -> r));

        // Engineering: converted values [24000, 76200, 100000]
        // avg = 200200/3 = 66733.333...
        SalaryStatsByGroupDto eng = byDept.get("Engineering");
        assertNotNull(eng, "Engineering should be present");
        assertEquals(3, eng.getCount());
        assertBigDecimalEquals("66733.33", eng.getAvgSalaryUsd(), "Engineering avg");
        assertBigDecimalEquals("24000.00", eng.getMinSalaryUsd(), "Engineering min");
        assertBigDecimalEquals("100000.00", eng.getMaxSalaryUsd(), "Engineering max");
        assertBigDecimalEquals("76200.00", eng.getMedianSalaryUsd(), "Engineering median");

        // Sales: converted values [18000, 63500, 80000]
        // avg = 161500/3 = 53833.333...
        SalaryStatsByGroupDto sales = byDept.get("Sales");
        assertNotNull(sales, "Sales should be present");
        assertEquals(3, sales.getCount());
        assertBigDecimalEquals("53833.33", sales.getAvgSalaryUsd(), "Sales avg");
        assertBigDecimalEquals("18000.00", sales.getMinSalaryUsd(), "Sales min");
        assertBigDecimalEquals("80000.00", sales.getMaxSalaryUsd(), "Sales max");
        assertBigDecimalEquals("63500.00", sales.getMedianSalaryUsd(), "Sales median");
    }

    @Test
    @DisplayName("headcount-by-country: employee count per country")
    void testHeadcountByCountry() {
        List<HeadcountByCountryDto> results = analyticsService.getHeadcountByCountry();

        assertEquals(3, results.size(), "Should have 3 countries");

        Map<String, Long> headcountMap = results.stream()
                .collect(Collectors.toMap(HeadcountByCountryDto::getCountry, HeadcountByCountryDto::getCount));

        assertEquals(2L, headcountMap.get("India"), "India headcount");
        assertEquals(2L, headcountMap.get("UK"), "UK headcount");
        assertEquals(2L, headcountMap.get("US"), "US headcount");
    }

    @Test
    @DisplayName("total-payroll: org-wide total and per-country breakdown in USD")
    void testTotalPayroll() {
        TotalPayrollDto result = analyticsService.getTotalPayroll();

        // Total = 100000 + 80000 + 76200 + 63500 + 24000 + 18000 = 361700.00
        assertBigDecimalEquals("361700.00", result.getTotalPayrollUsd(), "total payroll");
        assertEquals(6L, result.getTotalEmployees(), "total employee count");
        assertEquals("USD", result.getBaseCurrency());

        List<TotalPayrollDto.CountryPayroll> breakdown = result.getByCountry();
        assertEquals(3, breakdown.size(), "Should have 3 countries in breakdown");

        Map<String, TotalPayrollDto.CountryPayroll> byCountry = breakdown.stream()
                .collect(Collectors.toMap(TotalPayrollDto.CountryPayroll::getCountry, c -> c));

        // India: 24000 + 18000 = 42000
        assertBigDecimalEquals("42000.00", byCountry.get("India").getPayrollUsd(), "India payroll");
        assertEquals(2L, byCountry.get("India").getCount());

        // UK: 76200 + 63500 = 139700
        assertBigDecimalEquals("139700.00", byCountry.get("UK").getPayrollUsd(), "UK payroll");
        assertEquals(2L, byCountry.get("UK").getCount());

        // US: 100000 + 80000 = 180000
        assertBigDecimalEquals("180000.00", byCountry.get("US").getPayrollUsd(), "US payroll");
        assertEquals(2L, byCountry.get("US").getCount());
    }

    @Test
    @DisplayName("inactive employees are excluded from all analytics")
    void testInactiveEmployeesExcluded() {
        // Deactivate Alice (US, Engineering, $100k USD)
        Employee alice = employeeRepository.findByEmployeeCode("TEST-001").orElseThrow();
        alice.setActive(false);
        employeeRepository.save(alice);

        // Total payroll should now exclude Alice's 100000 USD
        // New total = 80000 + 76200 + 63500 + 24000 + 18000 = 261700.00
        TotalPayrollDto payroll = analyticsService.getTotalPayroll();
        assertBigDecimalEquals("261700.00", payroll.getTotalPayrollUsd(), "total after deactivation");
        assertEquals(5L, payroll.getTotalEmployees());

        // US headcount should drop to 1
        Map<String, Long> headcount = analyticsService.getHeadcountByCountry().stream()
                .collect(Collectors.toMap(HeadcountByCountryDto::getCountry, HeadcountByCountryDto::getCount));
        assertEquals(1L, headcount.get("US"), "US headcount after deactivation");
    }

    /**
     * Helper: assert two BigDecimal values are equal at scale 2, with a tolerance of ±0.01
     * to handle minor floating-point differences from database AVG() computation.
     */
    private void assertBigDecimalEquals(String expected, BigDecimal actual, String label) {
        BigDecimal exp = new BigDecimal(expected).setScale(2, RoundingMode.HALF_UP);
        BigDecimal act = actual.setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = exp.subtract(act).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.01")) <= 0,
                String.format("%s: expected %s but got %s (diff=%s)", label, exp, act, diff));
    }
}
