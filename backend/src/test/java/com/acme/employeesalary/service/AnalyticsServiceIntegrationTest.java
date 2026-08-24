package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.HeadcountByCountryDto;
import com.acme.employeesalary.dto.SalaryByCountryDto;
import com.acme.employeesalary.dto.SalaryByDepartmentDto;
import com.acme.employeesalary.dto.TotalPayrollDto;
import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.ExchangeRate;
import com.acme.employeesalary.repository.EmployeeRepository;
import com.acme.employeesalary.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AnalyticsServiceIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @BeforeEach
    void setUpKnownFixedDataset() {
        // 1. Seed fixed known exchange rates against USD
        exchangeRateRepository.save(ExchangeRate.builder()
                .currencyCode("USD").rateToBase(new BigDecimal("1.000000")).baseCurrency("USD").updatedAt(Instant.now()).build());
        exchangeRateRepository.save(ExchangeRate.builder()
                .currencyCode("GBP").rateToBase(new BigDecimal("1.270000")).baseCurrency("USD").updatedAt(Instant.now()).build());
        exchangeRateRepository.save(ExchangeRate.builder()
                .currencyCode("EUR").rateToBase(new BigDecimal("1.080000")).baseCurrency("USD").updatedAt(Instant.now()).build());

        // 2. Seed 6 active employees + 1 inactive employee across 3 countries and 2 departments:
        // Emp 1: US, Engineering, USD 100,000 -> Converted USD = 100,000.00
        employeeRepository.save(Employee.builder()
                .employeeCode("E1").name("Alice US").country("US").department("Engineering")
                .currency("USD").currentSalary(new BigDecimal("100000.00")).active(true).build());

        // Emp 2: US, Engineering, USD 120,000 -> Converted USD = 120,000.00
        employeeRepository.save(Employee.builder()
                .employeeCode("E2").name("Bob US").country("US").department("Engineering")
                .currency("USD").currentSalary(new BigDecimal("120000.00")).active(true).build());

        // Emp 3: US, Sales, USD 80,000 -> Converted USD = 80,000.00
        employeeRepository.save(Employee.builder()
                .employeeCode("E3").name("Charlie US").country("US").department("Sales")
                .currency("USD").currentSalary(new BigDecimal("80000.00")).active(true).build());

        // Emp 4: UK, Engineering, GBP 50,000 -> Converted USD = 50,000 * 1.27 = 63,500.00
        employeeRepository.save(Employee.builder()
                .employeeCode("E4").name("David UK").country("UK").department("Engineering")
                .currency("GBP").currentSalary(new BigDecimal("50000.00")).active(true).build());

        // Emp 5: UK, Sales, GBP 60,000 -> Converted USD = 60,000 * 1.27 = 76,200.00
        employeeRepository.save(Employee.builder()
                .employeeCode("E5").name("Emma UK").country("UK").department("Sales")
                .currency("GBP").currentSalary(new BigDecimal("60000.00")).active(true).build());

        // Emp 6: Germany, Engineering, EUR 70,000 -> Converted USD = 70,000 * 1.08 = 75,600.00
        employeeRepository.save(Employee.builder()
                .employeeCode("E6").name("Frank DE").country("Germany").department("Engineering")
                .currency("EUR").currentSalary(new BigDecimal("70000.00")).active(true).build());

        // Emp 7 (INACTIVE): US, Engineering, USD 999,999 -> Must be excluded from active calculations
        employeeRepository.save(Employee.builder()
                .employeeCode("E7").name("Inactive Emp").country("US").department("Engineering")
                .currency("USD").currentSalary(new BigDecimal("999999.00")).active(false).build());
    }

    @Test
    @DisplayName("getSalaryByCountry computes exact hand-calculated USD aggregates and medians per country")
    void testSalaryByCountryMath() {
        List<SalaryByCountryDto> list = analyticsService.getSalaryByCountry();

        assertThat(list).hasSize(3);

        // Germany: 1 emp [75,600.00]
        SalaryByCountryDto germany = list.stream().filter(c -> "Germany".equals(c.getCountry())).findFirst().orElseThrow();
        assertThat(germany.getEmployeeCount()).isEqualTo(1L);
        assertThat(germany.getMinSalaryUsd()).isEqualByComparingTo("75600.00");
        assertThat(germany.getMaxSalaryUsd()).isEqualByComparingTo("75600.00");
        assertThat(germany.getAvgSalaryUsd()).isEqualByComparingTo("75600.00");
        assertThat(germany.getMedianSalaryUsd()).isEqualByComparingTo("75600.00");

        // UK: 2 emps [63,500.00, 76,200.00]
        // Avg: (63500 + 76200) / 2 = 69,850.00
        // Median: (63500 + 76200) / 2 = 69,850.00
        SalaryByCountryDto uk = list.stream().filter(c -> "UK".equals(c.getCountry())).findFirst().orElseThrow();
        assertThat(uk.getEmployeeCount()).isEqualTo(2L);
        assertThat(uk.getMinSalaryUsd()).isEqualByComparingTo("63500.00");
        assertThat(uk.getMaxSalaryUsd()).isEqualByComparingTo("76200.00");
        assertThat(uk.getAvgSalaryUsd()).isEqualByComparingTo("69850.00");
        assertThat(uk.getMedianSalaryUsd()).isEqualByComparingTo("69850.00");

        // US: 3 emps [80,000.00, 100,000.00, 120,000.00]
        // Avg: (80000 + 100000 + 120000) / 3 = 100,000.00
        // Median: 100,000.00
        SalaryByCountryDto us = list.stream().filter(c -> "US".equals(c.getCountry())).findFirst().orElseThrow();
        assertThat(us.getEmployeeCount()).isEqualTo(3L);
        assertThat(us.getMinSalaryUsd()).isEqualByComparingTo("80000.00");
        assertThat(us.getMaxSalaryUsd()).isEqualByComparingTo("120000.00");
        assertThat(us.getAvgSalaryUsd()).isEqualByComparingTo("100000.00");
        assertThat(us.getMedianSalaryUsd()).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("getSalaryByDepartment computes exact hand-calculated USD aggregates and medians per department")
    void testSalaryByDepartmentMath() {
        List<SalaryByDepartmentDto> list = analyticsService.getSalaryByDepartment();

        assertThat(list).hasSize(2);

        // Engineering: 4 emps [63,500.00, 75,600.00, 100,000.00, 120,000.00]
        // Avg: (63500 + 75600 + 100000 + 120000) / 4 = 89,775.00
        // Median: (75600 + 100000) / 2 = 87,800.00
        SalaryByDepartmentDto eng = list.stream().filter(d -> "Engineering".equals(d.getDepartment())).findFirst().orElseThrow();
        assertThat(eng.getEmployeeCount()).isEqualTo(4L);
        assertThat(eng.getMinSalaryUsd()).isEqualByComparingTo("63500.00");
        assertThat(eng.getMaxSalaryUsd()).isEqualByComparingTo("120000.00");
        assertThat(eng.getAvgSalaryUsd()).isEqualByComparingTo("89775.00");
        assertThat(eng.getMedianSalaryUsd()).isEqualByComparingTo("87800.00");

        // Sales: 2 emps [76,200.00, 80,000.00]
        // Avg: (76200 + 80000) / 2 = 78,100.00
        // Median: (76200 + 80000) / 2 = 78,100.00
        SalaryByDepartmentDto sales = list.stream().filter(d -> "Sales".equals(d.getDepartment())).findFirst().orElseThrow();
        assertThat(sales.getEmployeeCount()).isEqualTo(2L);
        assertThat(sales.getMinSalaryUsd()).isEqualByComparingTo("76200.00");
        assertThat(sales.getMaxSalaryUsd()).isEqualByComparingTo("80000.00");
        assertThat(sales.getAvgSalaryUsd()).isEqualByComparingTo("78100.00");
        assertThat(sales.getMedianSalaryUsd()).isEqualByComparingTo("78100.00");
    }

    @Test
    @DisplayName("getHeadcountByCountry returns exact employee distribution and percentages")
    void testHeadcountByCountry() {
        List<HeadcountByCountryDto> list = analyticsService.getHeadcountByCountry();

        assertThat(list).hasSize(3);

        HeadcountByCountryDto us = list.get(0);
        assertThat(us.getCountry()).isEqualTo("US");
        assertThat(us.getHeadcount()).isEqualTo(3L);
        assertThat(us.getPercentage()).isEqualTo(50.0);

        HeadcountByCountryDto uk = list.get(1);
        assertThat(uk.getCountry()).isEqualTo("UK");
        assertThat(uk.getHeadcount()).isEqualTo(2L);
        assertThat(uk.getPercentage()).isEqualTo(33.33);

        HeadcountByCountryDto de = list.get(2);
        assertThat(de.getCountry()).isEqualTo("Germany");
        assertThat(de.getHeadcount()).isEqualTo(1L);
        assertThat(de.getPercentage()).isEqualTo(16.67);
    }

    @Test
    @DisplayName("getTotalPayroll computes exact org-wide total USD payroll and country breakdown")
    void testTotalPayroll() {
        TotalPayrollDto dto = analyticsService.getTotalPayroll();

        // Expected total: 100,000 + 120,000 + 80,000 + 63,500 + 76,200 + 75,600 = 515,300.00 USD
        assertThat(dto.getTotalEmployees()).isEqualTo(6L);
        assertThat(dto.getTotalPayrollUsd()).isEqualByComparingTo("515300.00");

        assertThat(dto.getCountryBreakdown()).hasSize(3);

        TotalPayrollDto.CountryPayrollDto us = dto.getCountryBreakdown().stream().filter(c -> "US".equals(c.getCountry())).findFirst().orElseThrow();
        assertThat(us.getEmployeeCount()).isEqualTo(3L);
        assertThat(us.getTotalPayrollUsd()).isEqualByComparingTo("300000.00");

        TotalPayrollDto.CountryPayrollDto uk = dto.getCountryBreakdown().stream().filter(c -> "UK".equals(c.getCountry())).findFirst().orElseThrow();
        assertThat(uk.getEmployeeCount()).isEqualTo(2L);
        assertThat(uk.getTotalPayrollUsd()).isEqualByComparingTo("139700.00");

        TotalPayrollDto.CountryPayrollDto de = dto.getCountryBreakdown().stream().filter(c -> "Germany".equals(c.getCountry())).findFirst().orElseThrow();
        assertThat(de.getEmployeeCount()).isEqualTo(1L);
        assertThat(de.getTotalPayrollUsd()).isEqualByComparingTo("75600.00");
    }
}
