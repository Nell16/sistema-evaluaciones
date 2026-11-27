package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.universidad.tests.domain.model.BaremoBFA;
import com.universidad.tests.domain.model.EstadoCeldaBaremo;
import com.universidad.tests.domain.model.RangoBaremoBFA;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BaremoBFAIntegrationTest {
    @Autowired private BaremoBFARepository baremos;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void guardaBaremoYRangosEnLasTablasMigradas() {
        String nombre = "BFA_INT_" + UUID.randomUUID().toString().replace("-", "");
        BaremoBFA guardado =
                baremos.guardar(
                        new BaremoBFA(
                                null,
                                nombre,
                                "v1",
                                1992,
                                "Poblacion sintetica",
                                "MANUAL-BFA",
                                List.of(
                                        new RangoBaremoBFA(
                                                "VOC1",
                                                5,
                                                1,
                                                4,
                                                EstadoCeldaBaremo.RANGO,
                                                null,
                                                "p. 1"),
                                        new RangoBaremoBFA(
                                                "VOC1",
                                                10,
                                                5,
                                                8,
                                                EstadoCeldaBaremo.RANGO,
                                                null,
                                                "p. 1"))));

        assertThat(guardado.idBaremo()).isPositive();
        assertThat(baremos.existePorNombreYVersion(nombre, "v1")).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM rango_baremo_bfa WHERE id_baremo = ?",
                                Integer.class,
                                guardado.idBaremo()))
                .isEqualTo(2);
    }
}
