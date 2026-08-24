package com.acme.employeesalary.seed;

import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.ExchangeRate;
import com.acme.employeesalary.entity.SalaryHistory;
import com.acme.employeesalary.repository.EmployeeRepository;
import com.acme.employeesalary.repository.ExchangeRateRepository;
import com.acme.employeesalary.repository.SalaryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryHistoryRepository salaryHistoryRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Test
    @DisplayName("DataSeeder correctly seeds small N=50 employees, salary histories, and 8 exchange rates")
    void seedDataShouldPopulateCorrectCountsAndShapes() {
        int testCount = 50;
        dataSeeder.seedData(testCount);

        // 1. Verify Exchange Rates count and USD rate
        long fxCount = exchangeRateRepository.count();
        assertThat(fxCount).isEqualTo(8);

        Optional<ExchangeRate> usdRate = exchangeRateRepository.findByCurrencyCode("USD");
        assertThat(usdRate).isPresent();
        assertThat(usdRate.get().getRateToBase()).isEqualByComparingTo("1.000000");

        Optional<ExchangeRate> gbpRate = exchangeRateRepository.findByCurrencyCode("GBP");
        assertThat(gbpRate).isPresent();
        assertThat(gbpRate.get().getRateToBase()).isEqualByComparingTo("1.270000");

        // 2. Verify Employee count
        long empCount = employeeRepository.count();
        assertThat(empCount).isEqualTo(testCount);

        // 3. Verify Salary History count matches employee count exactly (1 initial history per employee)
        long historyCount = salaryHistoryRepository.count();
        assertThat(historyCount).isEqualTo(testCount);

        // 4. Verify data shapes on sampled employees
        List<Employee> employees = employeeRepository.findAll();
        for (Employee emp : employees) {
            assertThat(emp.getEmployeeCode()).isNotNull().startsWith("EMP-");
            assertThat(emp.getName()).isNotBlank();
            assertThat(emp.getCountry()).isNotBlank();
            assertThat(emp.getDepartment()).isNotBlank();
            assertThat(emp.getCurrency()).isEqualTo(DataSeeder.COUNTRY_CURRENCY_MAP.get(emp.getCountry()));
            assertThat(emp.getCurrentSalary()).isNotNull().isGreaterThan(BigDecimal.ZERO);
            assertThat(emp.isActive()).isTrue();

            // Check matching salary history
            List<SalaryHistory> histories = salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDescChangedAtDesc(emp.getId());
            assertThat(histories).hasSize(1);
            SalaryHistory initialHistory = histories.get(0);
            assertThat(initialHistory.getAmount()).isEqualByComparingTo(emp.getCurrentSalary());
            assertThat(initialHistory.getCurrency()).isEqualTo(emp.getCurrency());
        }

        // 5. Test idempotency: calling seedData again does not add duplicate records
        dataSeeder.seedData(testCount);
        assertThat(employeeRepository.count()).isEqualTo(testCount);
        assertThat(salaryHistoryRepository.count()).isEqualTo(testCount);
        assertThat(exchangeRateRepository.count()).isEqualTo(8);
    }
}
