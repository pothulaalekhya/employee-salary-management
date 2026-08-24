package com.acme.employeesalary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Employee entity representing an organization employee and their current compensation.
 * Note on schema generation: Hibernate ddl-auto=update is used here for simplicity in a
 * take-home evaluation environment. In a production enterprise system, versioned database
 * migration tools such as Flyway or Liquibase would be used for migration control and auditability.
 */
@Entity
@Table(
        name = "employee",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_code", columnNames = "employee_code")
        },
        indexes = {
                @Index(name = "idx_employee_country", columnList = "country"),
                @Index(name = "idx_employee_department", columnList = "department"),
                @Index(name = "idx_employee_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, length = 50, unique = true)
    private String employeeCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "current_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentSalary;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
