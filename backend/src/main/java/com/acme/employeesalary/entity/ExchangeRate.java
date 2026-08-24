package com.acme.employeesalary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
 * ExchangeRate entity representing fixed seed exchange rates to base reporting currency (USD).
 */
@Entity
@Table(
        name = "exchange_rate",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exchange_rate_currency", columnNames = "currency_code")
        },
        indexes = {
                @Index(name = "idx_exchange_rate_currency", columnList = "currency_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", nullable = false, length = 3, unique = true)
    private String currencyCode;

    /**
     * Rate to multiply the local currency amount by in order to obtain the base-currency (USD) value.
     */
    @Column(name = "rate_to_base", nullable = false, precision = 12, scale = 6)
    private BigDecimal rateToBase;

    @Column(name = "base_currency", nullable = false, length = 3)
    @Builder.Default
    private String baseCurrency = "USD";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void onSaveOrUpdate() {
        this.updatedAt = Instant.now();
        if (this.baseCurrency == null) {
            this.baseCurrency = "USD";
        }
    }
}
