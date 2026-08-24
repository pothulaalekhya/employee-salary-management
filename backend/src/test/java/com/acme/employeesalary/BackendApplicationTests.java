package com.acme.employeesalary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class BackendApplicationTests {

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
    }
}
