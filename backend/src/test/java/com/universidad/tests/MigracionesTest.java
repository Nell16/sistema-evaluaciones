package com.universidad.tests;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MigracionesTest {
    @Autowired
    private Flyway flyway;

    @Test
    void todasLasMigracionesDeDatabaseMigrationsEstanAplicadas() {
        assertThat(flyway.info().pending()).isEmpty();
    }
}
