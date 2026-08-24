package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Employee, Long> {

    interface SalaryAggregationProjection {
        String getGroupKey();
        String getCurrency();
        Long getEmpCount();
        BigDecimal getAvgSalary();
        BigDecimal getMinSalary();
        BigDecimal getMaxSalary();
    }

    interface MedianProjection {
        String getGroupKey();
        BigDecimal getMedianSalary();
    }

    interface HeadcountProjection {
        String getCountry();
        Long getHeadcount();
    }

    interface TotalPayrollProjection {
        BigDecimal getTotalPayroll();
        Long getTotalEmployees();
    }

    interface CountryPayrollProjection {
        String getCountry();
        BigDecimal getTotalPayroll();
        Long getEmployeeCount();
    }

    @Query(value = """
            SELECT e.country AS groupKey,
                   e.currency AS currency,
                   COUNT(e.id) AS empCount,
                   ROUND(AVG(e.current_salary * er.rate_to_base), 2) AS avgSalary,
                   ROUND(MIN(e.current_salary * er.rate_to_base), 2) AS minSalary,
                   ROUND(MAX(e.current_salary * er.rate_to_base), 2) AS maxSalary
            FROM employee e
            JOIN exchange_rate er ON e.currency = er.currency_code
            WHERE e.active = true
            GROUP BY e.country, e.currency
            ORDER BY e.country
            """, nativeQuery = true)
    List<SalaryAggregationProjection> findSalaryByCountryStats();

    /*
     * SQL MEDIAN CALCULATION TECHNIQUE:
     * MySQL 8.0+ and H2 2.x support window functions and Common Table Expressions (CTE).
     * To compute the exact median without pulling all rows into JVM memory:
     * 1. Assign a row number ordered by converted USD salary per partition (country).
     * 2. Compute the total count per partition.
     * 3. Filter rows matching the 1-based middle index:
     *    - For odd count N: FLOOR((N+1)/2) == CEIL((N+1)/2), selecting the single median row.
     *    - For even count N: FLOOR((N+1)/2) and CEIL((N+1)/2) select the two middle rows, and AVG() computes their midpoint.
     */
    @Query(value = """
            WITH ranked AS (
                SELECT e.country AS group_key,
                       (e.current_salary * er.rate_to_base) AS converted_salary,
                       ROW_NUMBER() OVER (PARTITION BY e.country ORDER BY (e.current_salary * er.rate_to_base)) AS row_num,
                       COUNT(*) OVER (PARTITION BY e.country) AS total_count
                FROM employee e
                JOIN exchange_rate er ON e.currency = er.currency_code
                WHERE e.active = true
            )
            SELECT group_key AS groupKey,
                   ROUND(AVG(converted_salary), 2) AS medianSalary
            FROM ranked
            WHERE row_num IN (FLOOR((total_count + 1.0) / 2.0), CEIL((total_count + 1.0) / 2.0))
            GROUP BY group_key
            """, nativeQuery = true)
    List<MedianProjection> findMedianSalaryByCountry();

    @Query(value = """
            SELECT e.department AS groupKey,
                   '' AS currency,
                   COUNT(e.id) AS empCount,
                   ROUND(AVG(e.current_salary * er.rate_to_base), 2) AS avgSalary,
                   ROUND(MIN(e.current_salary * er.rate_to_base), 2) AS minSalary,
                   ROUND(MAX(e.current_salary * er.rate_to_base), 2) AS maxSalary
            FROM employee e
            JOIN exchange_rate er ON e.currency = er.currency_code
            WHERE e.active = true
            GROUP BY e.department
            ORDER BY e.department
            """, nativeQuery = true)
    List<SalaryAggregationProjection> findSalaryByDepartmentStats();

    @Query(value = """
            WITH ranked AS (
                SELECT e.department AS group_key,
                       (e.current_salary * er.rate_to_base) AS converted_salary,
                       ROW_NUMBER() OVER (PARTITION BY e.department ORDER BY (e.current_salary * er.rate_to_base)) AS row_num,
                       COUNT(*) OVER (PARTITION BY e.department) AS total_count
                FROM employee e
                JOIN exchange_rate er ON e.currency = er.currency_code
                WHERE e.active = true
            )
            SELECT group_key AS groupKey,
                   ROUND(AVG(converted_salary), 2) AS medianSalary
            FROM ranked
            WHERE row_num IN (FLOOR((total_count + 1.0) / 2.0), CEIL((total_count + 1.0) / 2.0))
            GROUP BY group_key
            """, nativeQuery = true)
    List<MedianProjection> findMedianSalaryByDepartment();

    @Query(value = """
            SELECT e.country AS country,
                   COUNT(e.id) AS headcount
            FROM employee e
            WHERE e.active = true
            GROUP BY e.country
            ORDER BY headcount DESC, e.country ASC
            """, nativeQuery = true)
    List<HeadcountProjection> findHeadcountByCountry();

    @Query(value = """
            SELECT ROUND(COALESCE(SUM(e.current_salary * er.rate_to_base), 0), 2) AS totalPayroll,
                   COUNT(e.id) AS totalEmployees
            FROM employee e
            JOIN exchange_rate er ON e.currency = er.currency_code
            WHERE e.active = true
            """, nativeQuery = true)
    TotalPayrollProjection findTotalPayroll();

    @Query(value = """
            SELECT e.country AS country,
                   ROUND(COALESCE(SUM(e.current_salary * er.rate_to_base), 0), 2) AS totalPayroll,
                   COUNT(e.id) AS employeeCount
            FROM employee e
            JOIN exchange_rate er ON e.currency = er.currency_code
            WHERE e.active = true
            GROUP BY e.country
            ORDER BY totalPayroll DESC
            """, nativeQuery = true)
    List<CountryPayrollProjection> findPayrollBreakdownByCountry();
}
