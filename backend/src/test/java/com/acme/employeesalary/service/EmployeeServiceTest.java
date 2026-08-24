package com.acme.employeesalary.service;

import com.acme.employeesalary.dto.EmployeeRequestDto;
import com.acme.employeesalary.dto.EmployeeResponseDto;
import com.acme.employeesalary.dto.EmployeeUpdateRequestDto;
import com.acme.employeesalary.dto.SalaryHistoryResponseDto;
import com.acme.employeesalary.dto.SalaryUpdateRequestDto;
import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.SalaryHistory;
import com.acme.employeesalary.exception.ResourceNotFoundException;
import com.acme.employeesalary.exception.ValidationException;
import com.acme.employeesalary.repository.EmployeeRepository;
import com.acme.employeesalary.repository.ExchangeRateRepository;
import com.acme.employeesalary.repository.SalaryHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryHistoryRepository salaryHistoryRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee sampleEmployee;

    @BeforeEach
    void setUp() {
        sampleEmployee = Employee.builder()
                .id(1L)
                .employeeCode("EMP-001")
                .name("Sarah Connor")
                .country("US")
                .department("Engineering")
                .title("Senior Engineer")
                .currency("USD")
                .currentSalary(new BigDecimal("120000.00"))
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getEmployees returns paginated EmployeeResponseDto")
    void getEmployeesShouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> employeePage = new PageImpl<>(List.of(sampleEmployee), pageable, 1);

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(employeePage);

        Page<EmployeeResponseDto> result = employeeService.getEmployees("US", "Engineering", null, null, "Sarah", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmployeeCode()).isEqualTo("EMP-001");
        assertThat(result.getContent().get(0).getName()).isEqualTo("Sarah Connor");
    }

    @Test
    @DisplayName("getEmployeeById returns EmployeeResponseDto for active employee")
    void getEmployeeByIdShouldReturnActiveEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        EmployeeResponseDto dto = employeeService.getEmployeeById(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Sarah Connor");
    }

    @Test
    @DisplayName("getEmployeeById throws ResourceNotFoundException if employee inactive")
    void getEmployeeByIdShouldThrowIfInactive() {
        sampleEmployee.setActive(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(1L));
    }

    @Test
    @DisplayName("getEmployeeById throws ResourceNotFoundException if employee not found")
    void getEmployeeByIdShouldThrowIfNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(999L));
    }

    @Test
    @DisplayName("getSalaryHistory returns descending list of salary records")
    void getSalaryHistoryShouldReturnDescendingList() {
        SalaryHistory h1 = SalaryHistory.builder()
                .id(10L)
                .employee(sampleEmployee)
                .amount(new BigDecimal("120000.00"))
                .currency("USD")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .changedAt(Instant.now())
                .note("Promotion")
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDescChangedAtDesc(1L))
                .thenReturn(List.of(h1));

        List<SalaryHistoryResponseDto> history = employeeService.getSalaryHistory(1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAmount()).isEqualByComparingTo("120000.00");
        assertThat(history.get(0).getNote()).isEqualTo("Promotion");
    }

    @Test
    @DisplayName("createEmployee saves Employee and initial SalaryHistory in one transaction")
    void createEmployeeShouldSaveEmployeeAndSalaryHistory() {
        EmployeeRequestDto request = EmployeeRequestDto.builder()
                .name("John Miller")
                .country("UK")
                .department("Sales")
                .title("Account Exec")
                .currency("GBP")
                .initialSalary(new BigDecimal("65000.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .note("Hiring compensation")
                .build();

        when(exchangeRateRepository.existsByCurrencyCode("GBP")).thenReturn(true);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(2L);
            return emp;
        });

        EmployeeResponseDto response = employeeService.createEmployee(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getCurrency()).isEqualTo("GBP");

        ArgumentCaptor<SalaryHistory> historyCaptor = ArgumentCaptor.forClass(SalaryHistory.class);
        verify(salaryHistoryRepository).save(historyCaptor.capture());
        SalaryHistory capturedHistory = historyCaptor.getValue();
        assertThat(capturedHistory.getAmount()).isEqualByComparingTo("65000.00");
        assertThat(capturedHistory.getCurrency()).isEqualTo("GBP");
        assertThat(capturedHistory.getEffectiveDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    @DisplayName("createEmployee rejects unsupported currency with ValidationException")
    void createEmployeeShouldRejectUnsupportedCurrency() {
        EmployeeRequestDto request = EmployeeRequestDto.builder()
                .name("John Miller")
                .country("Unknown")
                .department("Sales")
                .currency("XYZ")
                .initialSalary(new BigDecimal("50000.00"))
                .build();

        when(exchangeRateRepository.existsByCurrencyCode("XYZ")).thenReturn(false);

        assertThrows(ValidationException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    @DisplayName("createEmployee rejects non-positive salary")
    void createEmployeeShouldRejectNonPositiveSalary() {
        EmployeeRequestDto request = EmployeeRequestDto.builder()
                .name("John Miller")
                .country("US")
                .department("Sales")
                .currency("USD")
                .initialSalary(new BigDecimal("-100.00"))
                .build();

        assertThrows(ValidationException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    @DisplayName("updateEmployee updates non-salary fields successfully")
    void updateEmployeeShouldUpdateNonSalaryFields() {
        EmployeeUpdateRequestDto updateDto = EmployeeUpdateRequestDto.builder()
                .name("Sarah Connor-Reese")
                .country("US")
                .department("Leadership")
                .title("Director of Engineering")
                .build();

        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeResponseDto response = employeeService.updateEmployee(1L, updateDto);

        assertThat(response.getName()).isEqualTo("Sarah Connor-Reese");
        assertThat(response.getDepartment()).isEqualTo("Leadership");
        assertThat(response.getTitle()).isEqualTo("Director of Engineering");
        assertThat(response.getCurrentSalary()).isEqualByComparingTo("120000.00"); // Unchanged
    }

    @Test
    @DisplayName("updateEmployee rejects salary modifications with 400 ValidationException")
    void updateEmployeeShouldRejectSalaryChange() {
        EmployeeUpdateRequestDto updateDto = EmployeeUpdateRequestDto.builder()
                .name("Sarah Connor")
                .country("US")
                .department("Engineering")
                .currentSalary(new BigDecimal("140000.00"))
                .build();

        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleEmployee));

        assertThrows(ValidationException.class, () -> employeeService.updateEmployee(1L, updateDto));
    }

    @Test
    @DisplayName("updateSalary inserts SalaryHistory and updates Employee currentSalary")
    void updateSalaryShouldRecordHistoryAndUpdateCurrentSalary() {
        SalaryUpdateRequestDto salaryDto = SalaryUpdateRequestDto.builder()
                .newSalary(new BigDecimal("135000.00"))
                .effectiveDate(LocalDate.of(2024, 7, 1))
                .note("Mid-year performance raise")
                .build();

        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeResponseDto response = employeeService.updateSalary(1L, salaryDto);

        assertThat(response.getCurrentSalary()).isEqualByComparingTo("135000.00");

        ArgumentCaptor<SalaryHistory> historyCaptor = ArgumentCaptor.forClass(SalaryHistory.class);
        verify(salaryHistoryRepository).save(historyCaptor.capture());
        SalaryHistory history = historyCaptor.getValue();
        assertThat(history.getAmount()).isEqualByComparingTo("135000.00");
        assertThat(history.getNote()).isEqualTo("Mid-year performance raise");
    }

    @Test
    @DisplayName("updateSalary rejects new salary identical to current salary")
    void updateSalaryShouldRejectIdenticalSalary() {
        SalaryUpdateRequestDto salaryDto = SalaryUpdateRequestDto.builder()
                .newSalary(new BigDecimal("120000.00"))
                .build();

        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleEmployee));

        assertThrows(ValidationException.class, () -> employeeService.updateSalary(1L, salaryDto));
    }

    @Test
    @DisplayName("updateSalary rejects negative salary")
    void updateSalaryShouldRejectNegativeSalary() {
        SalaryUpdateRequestDto salaryDto = SalaryUpdateRequestDto.builder()
                .newSalary(new BigDecimal("-500.00"))
                .build();

        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleEmployee));

        assertThrows(ValidationException.class, () -> employeeService.updateSalary(1L, salaryDto));
    }

    @Test
    @DisplayName("deleteEmployee soft-deletes employee by setting active=false")
    void deleteEmployeeShouldSoftDelete() {
        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleEmployee));

        employeeService.deleteEmployee(1L);

        assertThat(sampleEmployee.isActive()).isFalse();
        verify(employeeRepository).save(sampleEmployee);
    }
}
