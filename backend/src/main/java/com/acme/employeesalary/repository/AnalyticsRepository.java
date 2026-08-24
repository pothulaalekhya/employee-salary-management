package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for pay analytics queries. All aggregation is done at the SQL level,
 * joining employee to exchange_rate on currency code and converting to base currency (USD)
 * via: employee.current_salary * exchange_rate.rate_to_base.
 *
 * No rows are pulled into Java for in-memory aggregation.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<Employee, Long> {

    // ─── salary-by-country: avg, min, max (SQL-level) ───────────────────
    @Query(value =
            "SELECT e.country AS grp, " +
            "       COUNT(*) AS cnt, " +
            "       AVG(e.current_salary * er.rate_to_base) AS avg_usd, " +
            "       MIN(e.current_salary * er.rate_to_base) AS min_usd, " +
            "       MAX(e.current_salary * er.rate_to_base) AS max_usd " +
            "FROM employee e " +
            "JOIN exchange_rate er ON e.currency = er.currency_code " +
            "WHERE e.active = true " +
            "GROUP BY e.country " +
            "ORDER BY e.country",
            nativeQuery = true)
    List<Object[]> findSalaryStatsByCountry();

    // ─── salary-by-department: avg, min, max (SQL-level) ────────────────
    @Query(value =
            "SELECT e.department AS grp, " +
            "       COUNT(*) AS cnt, " +
            "       AVG(e.current_salary * er.rate_to_base) AS avg_usd, " +
            "       MIN(e.current_salary * er.rate_to_base) AS min_usd, " +
            "       MAX(e.current_salary * er.rate_to_base) AS max_usd " +
            "FROM employee e " +
            "JOIN exchange_rate er ON e.currency = er.currency_code " +
            "WHERE e.active = true " +
            "GROUP BY e.department " +
            "ORDER BY e.department",
            nativeQuery = true)
    List<Object[]> findSalaryStatsByDepartment();

    /**
     * Median salary (converted to USD) per country using a window-function approach.
     *
     * MySQL has no built-in MEDIAN() aggregate. The technique used here:
     *   1. For each country group, assign a row number (rn) and count (cnt) to each
     *      employee's converted salary via ROW_NUMBER() OVER (PARTITION BY country ORDER BY converted_salary).
     *   2. The median row(s) are those where rn = FLOOR((cnt+1)/2) or rn = FLOOR((cnt+2)/2).
     *      - For an odd count N, both formulas yield the same row ((N+1)/2), giving one value.
     *      - For an even count N, they yield N/2 and N/2+1, and we average those two.
     *   3. AVG() over the selected row(s) gives the correct median for both odd and even counts.
     *
     * This is a standard "two-middle-rows" approach widely used in MySQL.
     */
    @Query(value =
            "SELECT ranked.country AS grp, AVG(ranked.converted_salary) AS median_usd " +
            "FROM ( " +
            "    SELECT e.country, " +
            "           e.current_salary * er.rate_to_base AS converted_salary, " +
            "           ROW_NUMBER() OVER (PARTITION BY e.country ORDER BY e.current_salary * er.rate_to_base) AS rn, " +
            "           COUNT(*) OVER (PARTITION BY e.country) AS cnt " +
            "    FROM employee e " +
            "    JOIN exchange_rate er ON e.currency = er.currency_code " +
            "    WHERE e.active = true " +
            ") ranked " +
            "WHERE ranked.rn = FLOOR((ranked.cnt + 1) / 2) " +
            "   OR ranked.rn = FLOOR((ranked.cnt + 2) / 2) " +
            "GROUP BY ranked.country " +
            "ORDER BY ranked.country",
            nativeQuery = true)
    List<Object[]> findMedianSalaryByCountry();

    /**
     * Median salary (converted to USD) per department — same window-function technique
     * as findMedianSalaryByCountry(), partitioned by department instead.
     */
    @Query(value =
            "SELECT ranked.department AS grp, AVG(ranked.converted_salary) AS median_usd " +
            "FROM ( " +
            "    SELECT e.department, " +
            "           e.current_salary * er.rate_to_base AS converted_salary, " +
            "           ROW_NUMBER() OVER (PARTITION BY e.department ORDER BY e.current_salary * er.rate_to_base) AS rn, " +
            "           COUNT(*) OVER (PARTITION BY e.department) AS cnt " +
            "    FROM employee e " +
            "    JOIN exchange_rate er ON e.currency = er.currency_code " +
            "    WHERE e.active = true " +
            ") ranked " +
            "WHERE ranked.rn = FLOOR((ranked.cnt + 1) / 2) " +
            "   OR ranked.rn = FLOOR((ranked.cnt + 2) / 2) " +
            "GROUP BY ranked.department " +
            "ORDER BY ranked.department",
            nativeQuery = true)
    List<Object[]> findMedianSalaryByDepartment();

    // ─── headcount-by-country ───────────────────────────────────────────
    @Query(value =
            "SELECT e.country, COUNT(*) AS cnt " +
            "FROM employee e " +
            "WHERE e.active = true " +
            "GROUP BY e.country " +
            "ORDER BY e.country",
            nativeQuery = true)
    List<Object[]> findHeadcountByCountry();

    // ─── total-payroll org-wide ─────────────────────────────────────────
    @Query(value =
            "SELECT SUM(e.current_salary * er.rate_to_base) AS total_usd, " +
            "       COUNT(*) AS cnt " +
            "FROM employee e " +
            "JOIN exchange_rate er ON e.currency = er.currency_code " +
            "WHERE e.active = true",
            nativeQuery = true)
    Object[] findTotalPayroll();

    // ─── total-payroll breakdown by country ─────────────────────────────
    @Query(value =
            "SELECT e.country, " +
            "       COUNT(*) AS cnt, " +
            "       SUM(e.current_salary * er.rate_to_base) AS payroll_usd " +
            "FROM employee e " +
            "JOIN exchange_rate er ON e.currency = er.currency_code " +
            "WHERE e.active = true " +
            "GROUP BY e.country " +
            "ORDER BY e.country",
            nativeQuery = true)
    List<Object[]> findPayrollByCountry();
}
