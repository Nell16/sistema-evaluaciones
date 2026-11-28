package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.universidad.tests.application.service.AplicarBaremoBFAService;
import com.universidad.tests.application.service.AprobarBaremoBFAService;
import com.universidad.tests.application.service.ConversionPercentilBFAService.EstadoNormativo;
import com.universidad.tests.domain.model.BaremoBFA;
import com.universidad.tests.domain.model.EstadoCeldaBaremo;
import com.universidad.tests.domain.model.RangoBaremoBFA;
import com.universidad.tests.domain.port.AutorizacionBaremoPort;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import com.universidad.tests.infrastructure.persistence.entity.TestJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.TestJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionFormularioJpaRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BaremoConversionIntegrationTest {

    @Autowired private BaremoBFARepository baremos;
    @Autowired private TestJpaRepository tests;
    @Autowired private VersionFormularioJpaRepository versiones;
    @Autowired private AprobarBaremoBFAService aprobaciones;
    @Autowired private AplicarBaremoBFAService aplicacion;
    @MockitoBean private AutorizacionBaremoPort autorizacion;

    @Test
    void convierteSoloConBaremoAprobadoParaLaVersionYLasReglasExactas() {
        TestJpaEntity test =
                tests.saveAndFlush(
                        new TestJpaEntity(
                                "BFA-CONV-" + UUID.randomUUID(),
                                "Test ficticio",
                                "Datos sinteticos",
                                true));
        VersionFormularioJpaEntity version =
                versiones.saveAndFlush(
                        new VersionFormularioJpaEntity(test, "v1", null, "FUENTE-SINTETICA"));
        long idBaremo =
                baremos.guardar(
                                new BaremoBFA(
                                        null,
                                        "BFA-CONV-" + UUID.randomUUID(),
                                        "v1",
                                        2026,
                                        "Poblacion sintetica",
                                        "FUENTE-SINTETICA",
                                        List.of(
                                                new RangoBaremoBFA(
                                                        "VOC1",
                                                        40,
                                                        3,
                                                        5,
                                                        EstadoCeldaBaremo.RANGO,
                                                        null,
                                                        "tabla ficticia"))))
                        .idBaremo();
        long idVersion = version.getIdVersionFormulario();

        assertThat(
                        aplicacion
                                .aplicar(idVersion, "reglas-v1", "VOC1", 4)
                                .conversion()
                                .estadoNormativo())
                .isEqualTo(EstadoNormativo.SIN_BAREMO_VALIDADO);

        aprobaciones.aprobar(idBaremo, idVersion, "reglas-v1", "REVISION-SINTETICA");

        var resultado = aplicacion.aplicar(idVersion, "reglas-v1", "VOC1", 4);
        assertThat(resultado.idBaremo()).isEqualTo(idBaremo);
        assertThat(resultado.conversion().percentil()).isEqualTo(40);
        assertThat(resultado.conversion().idRango()).isPositive();
        assertThat(resultado.conversion().estadoNormativo()).isEqualTo(EstadoNormativo.CONVERTIDO);

        assertThat(
                        aplicacion
                                .aplicar(idVersion, "otras-reglas", "VOC1", 4)
                                .conversion()
                                .percentil())
                .isNull();
        assertThat(aplicacion.aplicar(idVersion, "reglas-v1", "VOC1", 9).conversion().percentil())
                .isNull();

        aprobaciones.retirar(idBaremo, idVersion, "reglas-v1");
        assertThat(
                        aplicacion
                                .aplicar(idVersion, "reglas-v1", "VOC1", 4)
                                .conversion()
                                .estadoNormativo())
                .isEqualTo(EstadoNormativo.SIN_BAREMO_VALIDADO);
    }
}
