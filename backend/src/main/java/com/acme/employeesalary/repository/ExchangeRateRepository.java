package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyCode(String currencyCode);

    boolean existsByCurrencyCode(String currencyCode);
}
