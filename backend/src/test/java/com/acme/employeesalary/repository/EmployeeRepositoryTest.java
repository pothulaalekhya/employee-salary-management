package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist and retrieve an employee successfully")
    void shouldSaveAndFindEmployee() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-1001")
                .name("Jane Doe")
                .country("US")
                .department("Engineering")
                .title("Staff Engineer")
                .currency("USD")
                .currentSalary(new BigDecimal("155000.00"))
                .active(true)
                .build();

        Employee saved = employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        Optional<Employee> found = employeeRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmployeeCode()).isEqualTo("EMP-1001");
        assertThat(found.get().getName()).isEqualTo("Jane Doe");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException on duplicate employeeCode")
    void shouldEnforceUniqueConstraintOnEmployeeCode() {
        Employee emp1 = Employee.builder()
                .employeeCode("EMP-UNIQUE")
                .name("Alex Smith")
                .country("UK")
                .department("Sales")
                .title("Account Exec")
                .currency("GBP")
                .currentSalary(new BigDecimal("75000.00"))
                .active(true)
                .build();
        employeeRepository.saveAndFlush(emp1);

        Employee emp2 = Employee.builder()
                .employeeCode("EMP-UNIQUE")
                .name("Bob Taylor")
                .country("US")
                .department("HR")
                .title("HR Manager")
                .currency("USD")
                .currentSalary(new BigDecimal("90000.00"))
                .active(true)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            employeeRepository.saveAndFlush(emp2);
        });
    }

    @Test
    @DisplayName("Should find active employees and ignore inactive ones when queried with findByIdAndActiveTrue")
    void shouldFindByIdAndActiveTrue() {
        Employee activeEmp = Employee.builder()
                .employeeCode("EMP-ACTIVE")
                .name("Active User")
                .country("US")
                .department("Engineering")
                .title("Developer")
                .currency("USD")
                .currentSalary(new BigDecimal("100000.00"))
                .active(true)
                .build();
        Employee inactiveEmp = Employee.builder()
                .employeeCode("EMP-INACTIVE")
                .name("Inactive User")
                .country("US")
                .department("Engineering")
                .title("Developer")
                .currency("USD")
                .currentSalary(new BigDecimal("100000.00"))
                .active(false)
                .build();

        Employee savedActive = employeeRepository.save(activeEmp);
        Employee savedInactive = employeeRepository.save(inactiveEmp);
        entityManager.flush();
        entityManager.clear();

        assertThat(employeeRepository.findByIdAndActiveTrue(savedActive.getId())).isPresent();
        assertThat(employeeRepository.findByIdAndActiveTrue(savedInactive.getId())).isEmpty();
    }
}
