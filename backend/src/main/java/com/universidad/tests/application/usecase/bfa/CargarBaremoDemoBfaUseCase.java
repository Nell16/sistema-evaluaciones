package com.universidad.tests.application.usecase.bfa;

import com.universidad.tests.domain.model.BaremoBFA;
import com.universidad.tests.domain.model.EstadoCeldaBaremo;
import com.universidad.tests.domain.model.RangoBaremoBFA;
import com.universidad.tests.domain.model.bfa.SubpruebaBfa;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga el baremo FICTICIO de la demo del BFA (BFA-BE-39): un juego de rangos inventados por cada
 * familia (factor). Los rangos y percentiles NO son normas reales y estan marcados como ficticios.
 * Escribe directo en el repositorio: el servicio de carga exige un permiso que en el arranque no
 * existe.
 */
@Service
public class CargarBaremoDemoBfaUseCase {

    public static final String NOMBRE_BAREMO_DEMO = "BAREMO DEMO FICTICIO";
    public static final String VERSION_BAREMO_DEMO = "DEMO-1";
    static final int ANIO_NORMA_DEMO = 2026;
    static final String POBLACION_DEMO =
            "Poblacion ficticia de demostracion (no es una norma real)";
    static final String FUENTE_BAREMO_DEMO = "demo/baremo-ficticio";
    static final String VALOR_ORIGINAL_DEMO = "FICTICIO";
    static final int PUNTAJE_MAXIMO_POR_SUBPRUEBA = 3;
    static final int PUNTAJE_MAXIMO_FV = 75;
    static final int MAXIMO_RANGOS_POR_FAMILIA = 5;

    private final BaremoBFARepository baremoRepository;

    public CargarBaremoDemoBfaUseCase(BaremoBFARepository baremoRepository) {
        this.baremoRepository = baremoRepository;
    }

    /** Devuelve true si creo el baremo y false si ya existia (no lo toca). */
    @Transactional
    public boolean cargarBaremo() {
        if (baremoRepository.existePorNombreYVersion(NOMBRE_BAREMO_DEMO, VERSION_BAREMO_DEMO)) {
            return false;
        }
        baremoRepository.guardar(
                new BaremoBFA(
                        null,
                        NOMBRE_BAREMO_DEMO,
                        VERSION_BAREMO_DEMO,
                        ANIO_NORMA_DEMO,
                        POBLACION_DEMO,
                        FUENTE_BAREMO_DEMO,
                        rangosDemo()));
        return true;
    }

    // Familias en orden oficial: VOC1, VOC2, S1 (S1 + S1b), S2, R1, R2, CV, N1, N2, FV
    static List<RangoBaremoBFA> rangosDemo() {
        List<RangoBaremoBFA> rangos = new ArrayList<>();
        for (Map.Entry<String, Integer> familia : puntajeMaximoPorFamilia().entrySet()) {
            rangos.addAll(rangosDeFamilia(familia.getKey(), familia.getValue()));
        }
        return rangos;
    }

    static Map<String, Integer> puntajeMaximoPorFamilia() {
        Map<String, Integer> maximos = new LinkedHashMap<>();
        for (SubpruebaBfa subprueba : SubpruebaBfa.values()) {
            int aporte =
                    subprueba == SubpruebaBfa.FV ? PUNTAJE_MAXIMO_FV : PUNTAJE_MAXIMO_POR_SUBPRUEBA;
            maximos.merge(subprueba.getFactor(), aporte, Integer::sum);
        }
        return maximos;
    }

    // Reparte 0..maximo en bandas contiguas sin solaparse, cada una con su percentil inventado
    static List<RangoBaremoBFA> rangosDeFamilia(String idFactor, int maximo) {
        int valores = maximo + 1;
        int bandas = Math.min(MAXIMO_RANGOS_POR_FAMILIA, valores);
        List<RangoBaremoBFA> rangos = new ArrayList<>();
        int inferior = 0;
        for (int i = 0; i < bandas; i++) {
            int restantes = valores - inferior;
            int ancho = (restantes + (bandas - i) - 1) / (bandas - i);
            int superior = inferior + ancho - 1;
            int percentil = Math.round(100f * (i + 1) / (bandas + 1));
            rangos.add(
                    new RangoBaremoBFA(
                            idFactor,
                            percentil,
                            inferior,
                            superior,
                            EstadoCeldaBaremo.RANGO,
                            VALOR_ORIGINAL_DEMO,
                            FUENTE_BAREMO_DEMO));
            inferior = superior + 1;
        }
        return rangos;
    }
}
