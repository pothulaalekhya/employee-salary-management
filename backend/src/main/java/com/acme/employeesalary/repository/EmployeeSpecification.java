package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification {

    public static Specification<Employee> withFilters(
            String country,
            String department,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            String name
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // By default, filter only active employees
            predicates.add(cb.isTrue(root.get("active")));

            if (country != null && !country.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("country")), country.trim().toLowerCase()));
            }

            if (department != null && !department.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase()));
            }

            if (minSalary != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("currentSalary"), minSalary));
            }

            if (maxSalary != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentSalary"), maxSalary));
            }

            if (name != null && !name.trim().isEmpty()) {
                String pattern = "%" + name.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
