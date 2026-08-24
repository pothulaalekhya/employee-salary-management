package com.acme.employeesalary.repository;

import com.acme.employeesalary.entity.ExchangeRate;
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
class ExchangeRateRepositoryTest {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and find exchange rate by currency code")
    void shouldSaveAndFindByCurrencyCode() {
        ExchangeRate rate = ExchangeRate.builder()
                .currencyCode("EUR")
                .rateToBase(new BigDecimal("1.085000"))
                .baseCurrency("USD")
                .build();

        exchangeRateRepository.save(rate);
        entityManager.flush();
        entityManager.clear();

        Optional<ExchangeRate> found = exchangeRateRepository.findByCurrencyCode("EUR");
        assertThat(found).isPresent();
        assertThat(found.get().getRateToBase()).isEqualByComparingTo("1.085000");
        assertThat(found.get().getBaseCurrency()).isEqualTo("USD");
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce unique constraint on currencyCode")
    void shouldEnforceUniqueCurrencyCode() {
        ExchangeRate rate1 = ExchangeRate.builder()
                .currencyCode("GBP")
                .rateToBase(new BigDecimal("1.270000"))
                .baseCurrency("USD")
                .build();
        exchangeRateRepository.saveAndFlush(rate1);

        ExchangeRate rate2 = ExchangeRate.builder()
                .currencyCode("GBP")
                .rateToBase(new BigDecimal("1.280000"))
                .baseCurrency("USD")
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            exchangeRateRepository.saveAndFlush(rate2);
        });
    }
}
