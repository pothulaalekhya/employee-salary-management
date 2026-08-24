package com.acme.employeesalary.seed;

import com.acme.employeesalary.entity.Employee;
import com.acme.employeesalary.entity.ExchangeRate;
import com.acme.employeesalary.entity.SalaryHistory;
import com.acme.employeesalary.repository.EmployeeRepository;
import com.acme.employeesalary.repository.ExchangeRateRepository;
import com.acme.employeesalary.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final Environment environment;

    // Supported Countries and their native currencies
    public static final Map<String, String> COUNTRY_CURRENCY_MAP = Map.of(
            "US", "USD",
            "UK", "GBP",
            "India", "INR",
            "Germany", "EUR",
            "Brazil", "BRL",
            "Japan", "JPY",
            "Australia", "AUD",
            "Canada", "CAD"
    );

    // Static illustrative exchange rates against base reporting currency USD per Incubyte requirements
    // (multiply local currency by rateToBase to get base USD value).
    // Note: These are static illustrative seed values, not live FX API rates.
    public static final Map<String, BigDecimal> STATIC_EXCHANGE_RATES = Map.of(
            "USD", new BigDecimal("1.000000"),
            "GBP", new BigDecimal("1.270000"),
            "EUR", new BigDecimal("1.080000"),
            "INR", new BigDecimal("0.012000"),
            "BRL", new BigDecimal("0.200000"),
            "JPY", new BigDecimal("0.006700"),
            "AUD", new BigDecimal("0.660000"),
            "CAD", new BigDecimal("0.730000")
    );

    public static final List<String> DEPARTMENTS = List.of(
            "Engineering", "Sales", "Marketing", "HR", "Finance", "Operations"
    );

    private static final Map<String, List<String>> DEPARTMENT_TITLES = Map.of(
            "Engineering", List.of("Software Engineer", "Senior Software Engineer", "Tech Lead", "Principal Engineer", "Engineering Manager", "QA Engineer", "DevOps Engineer"),
            "Sales", List.of("Sales Representative", "Account Executive", "Senior Account Executive", "Sales Manager", "VP of Sales", "Business Development Rep"),
            "Marketing", List.of("Marketing Specialist", "Content Strategist", "Product Marketing Manager", "SEO Specialist", "Marketing Director"),
            "HR", List.of("HR Coordinator", "HR Specialist", "Talent Acquisition Lead", "HR Business Partner", "Director of People"),
            "Finance", List.of("Financial Analyst", "Senior Financial Analyst", "Accounting Manager", "Controller", "Finance Director"),
            "Operations", List.of("Operations Specialist", "Operations Manager", "Supply Chain Analyst", "Director of Operations")
    );

    @Override
    public void run(ApplicationArguments args) {
        boolean hasSeedFlag = args.containsOption("seed")
                || Arrays.asList(args.getSourceArgs()).contains("--seed");
        boolean hasSeedProfile = environment.acceptsProfiles(Profiles.of("seed"));

        if (!hasSeedFlag && !hasSeedProfile) {
            log.info("[DataSeeder] Seed flag not provided (--seed or spring.profiles.active=seed). Skipping data generation.");
            return;
        }

        seedData(10000);
    }

    @Transactional
    public void seedData(int targetCount) {
        // IDEMPOTENCY CHECK:
        // If the database already contains employee records, skip seeding to avoid duplicate data or constraint violations.
        // This makes the seed script safe to re-run in CI, Docker startup, or local resets.
        if (employeeRepository.count() > 0) {
            log.info("[DataSeeder] Database already contains {} employees. Skipping seed execution.", employeeRepository.count());
            return;
        }

        log.info("[DataSeeder] Starting database seed process for {} employees...", targetCount);
        long startTime = System.currentTimeMillis();

        seedExchangeRates();
        seedEmployeesAndHistory(targetCount);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[DataSeeder] Seeding completed in {} ms. Total employees: {}, Total exchange rates: {}",
                duration, employeeRepository.count(), exchangeRateRepository.count());
    }

    public void seedExchangeRates() {
        if (exchangeRateRepository.count() > 0) {
            log.info("[DataSeeder] Exchange rates already exist. Skipping FX seed.");
            return;
        }

        List<ExchangeRate> rates = new ArrayList<>();
        STATIC_EXCHANGE_RATES.forEach((currency, rate) -> {
            ExchangeRate fx = ExchangeRate.builder()
                    .currencyCode(currency)
                    .rateToBase(rate)
                    .baseCurrency("USD")
                    .updatedAt(Instant.now())
                    .build();
            rates.add(fx);
        });

        exchangeRateRepository.saveAll(rates);
        log.info("[DataSeeder] Seeded {} exchange rate rows.", rates.size());
    }

    public void seedEmployeesAndHistory(int count) {
        Faker faker = new Faker(Locale.ENGLISH);
        Random random = new Random(42); // Fixed seed for reproducible generation
        List<String> countries = new ArrayList<>(COUNTRY_CURRENCY_MAP.keySet());

        int batchSize = 1000;
        List<Employee> employeeBatch = new ArrayList<>(batchSize);
        List<SalaryHistory> historyBatch = new ArrayList<>(batchSize);

        for (int i = 1; i <= count; i++) {
            String country = countries.get(random.nextInt(countries.size()));
            String currency = COUNTRY_CURRENCY_MAP.get(country);
            String department = DEPARTMENTS.get(random.nextInt(DEPARTMENTS.size()));
            List<String> titles = DEPARTMENT_TITLES.get(department);
            String title = titles.get(random.nextInt(titles.size()));
            String name = faker.name().fullName();
            String employeeCode = String.format("EMP-%05d", i);

            BigDecimal currentSalary = calculateRealisticSalary(country, department, title, random);
            LocalDate hireDate = LocalDate.now().minusDays(random.nextInt(1800) + 30); // Hired between 1 month and 5 years ago

            Employee employee = Employee.builder()
                    .employeeCode(employeeCode)
                    .name(name)
                    .country(country)
                    .department(department)
                    .title(title)
                    .currency(currency)
                    .currentSalary(currentSalary)
                    .active(true)
                    .createdAt(hireDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())
                    .build();

            employeeBatch.add(employee);

            if (employeeBatch.size() == batchSize || i == count) {
                // Save employee batch
                List<Employee> savedEmployees = employeeRepository.saveAll(employeeBatch);

                // Create initial SalaryHistory for each employee
                for (Employee emp : savedEmployees) {
                    SalaryHistory history = SalaryHistory.builder()
                            .employee(emp)
                            .amount(emp.getCurrentSalary())
                            .currency(emp.getCurrency())
                            .effectiveDate(hireDate)
                            .changedAt(emp.getCreatedAt())
                            .note("Initial compensation on record creation")
                            .build();
                    historyBatch.add(history);
                }

                salaryHistoryRepository.saveAll(historyBatch);

                employeeBatch.clear();
                historyBatch.clear();

                if (i % 2000 == 0 || i == count) {
                    log.info("[DataSeeder] Seeded {}/{} employees...", i, count);
                }
            }
        }
    }

    /**
     * Computes realistic compensation based on both country currency scale and department role seniority.
     */
    public BigDecimal calculateRealisticSalary(String country, String department, String title, Random random) {
        // Base ranges by country in native currency
        double baseMin;
        double baseMax;

        switch (country) {
            case "US":
                baseMin = 70_000;
                baseMax = 150_000;
                break;
            case "UK":
                baseMin = 45_000;
                baseMax = 95_000;
                break;
            case "Germany":
                baseMin = 50_000;
                baseMax = 105_000;
                break;
            case "India":
                baseMin = 800_000;
                baseMax = 3_000_000;
                break;
            case "Brazil":
                baseMin = 70_000;
                baseMax = 200_000;
                break;
            case "Japan":
                baseMin = 5_500_000;
                baseMax = 13_000_000;
                break;
            case "Australia":
                baseMin = 80_000;
                baseMax = 160_000;
                break;
            case "Canada":
                baseMin = 75_000;
                baseMax = 150_000;
                break;
            default:
                baseMin = 60_000;
                baseMax = 120_000;
                break;
        }

        // Department multiplier
        double deptMultiplier;
        switch (department) {
            case "Engineering":
                deptMultiplier = 1.25;
                break;
            case "Finance":
                deptMultiplier = 1.10;
                break;
            case "Sales":
                deptMultiplier = 1.05;
                break;
            case "Marketing":
                deptMultiplier = 0.95;
                break;
            case "Operations":
                deptMultiplier = 0.90;
                break;
            case "HR":
            default:
                deptMultiplier = 0.85;
                break;
        }

        // Seniority multiplier based on title
        double seniorityMultiplier = 1.0;
        if (title.contains("Director") || title.contains("VP")) {
            seniorityMultiplier = 1.50;
        } else if (title.contains("Manager") || title.contains("Principal") || title.contains("Lead")) {
            seniorityMultiplier = 1.30;
        } else if (title.contains("Senior")) {
            seniorityMultiplier = 1.15;
        }

        double randomFactor = 0.90 + (random.nextDouble() * 0.20); // 0.90 - 1.10
        double salary = baseMin + (random.nextDouble() * (baseMax - baseMin));
        salary = salary * deptMultiplier * seniorityMultiplier * randomFactor;

        // Round to 2 decimal places (or whole number for INR/JPY)
        BigDecimal result = BigDecimal.valueOf(salary).setScale(2, RoundingMode.HALF_UP);
        if ("JPY".equals(COUNTRY_CURRENCY_MAP.get(country)) || "INR".equals(COUNTRY_CURRENCY_MAP.get(country))) {
            result = result.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        }
        return result;
    }
}
