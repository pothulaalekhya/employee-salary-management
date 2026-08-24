package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.SalaryHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
class SalaryHistoryRepositoryTest {

    @Autowired
    private SalaryHistoryRepository salaryHistoryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find salary histories ordered by effective date descending")
    void shouldFindSalaryHistoriesOrderedByEffectiveDateDesc() {
        Employee employee = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-HIST-1")
                .name("Carlos Gomez")
                .country("US")
                .department("Engineering")
                .title("Software Engineer")
                .currency("USD")
                .currentSalary(new BigDecimal("130000.00"))
                .active(true)
                .build());

        SalaryHistory h1 = SalaryHistory.builder()
                .employee(employee)
                .amount(new BigDecimal("100000.00"))
                .currency("USD")
                .effectiveDate(LocalDate.of(2022, 1, 1))
                .note("Initial joining compensation")
                .build();

        SalaryHistory h2 = SalaryHistory.builder()
                .employee(employee)
                .amount(new BigDecimal("115000.00"))
                .currency("USD")
                .effectiveDate(LocalDate.of(2023, 6, 1))
                .note("Annual performance increment")
                .build();

        SalaryHistory h3 = SalaryHistory.builder()
                .employee(employee)
                .amount(new BigDecimal("130000.00"))
                .currency("USD")
                .effectiveDate(LocalDate.of(2024, 8, 1))
                .note("Promotion to Senior Engineer")
                .build();

        salaryHistoryRepository.saveAll(List.of(h1, h2, h3));
        entityManager.flush();
        entityManager.clear();

        List<SalaryHistory> histories = salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employee.getId());
        assertThat(histories).hasSize(3);
        assertThat(histories.get(0).getAmount()).isEqualByComparingTo("130000.00");
        assertThat(histories.get(0).getEffectiveDate()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(histories.get(1).getAmount()).isEqualByComparingTo("115000.00");
        assertThat(histories.get(2).getAmount()).isEqualByComparingTo("100000.00");
        assertThat(histories.get(0).getChangedAt()).isNotNull();
    }
}
