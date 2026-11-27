package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.BaremoBFA;
import com.universidad.tests.domain.model.EstadoCeldaBaremo;
import com.universidad.tests.domain.model.RangoBaremoBFA;
import com.universidad.tests.domain.port.AutorizacionBaremoPort;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CargarBaremoBFAService {
    private final BaremoBFARepository baremos;
    private final AutorizacionBaremoPort autorizacion;

    public CargarBaremoBFAService(
            BaremoBFARepository baremos, AutorizacionBaremoPort autorizacion) {
        this.baremos = baremos;
        this.autorizacion = autorizacion;
    }

    @Transactional
    public Long cargarDatos(DatosBaremo datos) {
        List<RangoBaremoBFA> rangos =
                datos.rangos().stream()
                        .map(
                                r ->
                                        new RangoBaremoBFA(
                                                r.idFactor(),
                                                r.percentil(),
                                                r.limiteInferior(),
                                                r.limiteSuperior(),
                                                EstadoCeldaBaremo.valueOf(r.estadoCelda()),
                                                r.valorOriginal(),
                                                r.localizadorFuente()))
                        .toList();
        return cargar(
                new BaremoBFA(
                        null,
                        datos.nombre(),
                        datos.version(),
                        datos.anioNorma(),
                        datos.poblacionReferencia(),
                        datos.referenciaFuente(),
                        rangos));
    }

    @Transactional
    public Long cargar(BaremoBFA baremo) {
        if (baremo == null
                || baremo.nombre() == null
                || baremo.nombre().isBlank()
                || baremo.version() == null
                || baremo.version().isBlank()
                || baremo.anioNorma() <= 0
                || baremo.rangos().isEmpty()) {
            throw new IllegalArgumentException("Faltan datos obligatorios del baremo");
        }
        autorizacion.exigirPermisoCargarBaremo();
        validarRangos(baremo.rangos());
        if (baremos.existePorNombreYVersion(baremo.nombre(), baremo.version())) {
            throw new BaremoDuplicadoException();
        }
        return baremos.guardar(baremo).idBaremo();
    }

    private void validarRangos(List<RangoBaremoBFA> rangos) {
        Set<String> celdas = new HashSet<>();
        Map<String, List<RangoBaremoBFA>> porFactor = new HashMap<>();
        for (RangoBaremoBFA rango : rangos) {
            if (rango == null
                    || rango.idFactor() == null
                    || rango.idFactor().isBlank()
                    || rango.percentil() < 1
                    || rango.percentil() > 99
                    || rango.estadoCelda() == null
                    || !celdas.add(rango.idFactor() + ":" + rango.percentil())) {
                throw new BaremoInvalidoException();
            }
            if (rango.estadoCelda() == EstadoCeldaBaremo.RANGO) {
                if (rango.limiteInferior() == null
                        || rango.limiteSuperior() == null
                        || rango.limiteInferior() > rango.limiteSuperior()) {
                    throw new BaremoInvalidoException();
                }
                porFactor
                        .computeIfAbsent(rango.idFactor(), ignored -> new java.util.ArrayList<>())
                        .add(rango);
            } else if (rango.limiteInferior() != null || rango.limiteSuperior() != null) {
                throw new BaremoInvalidoException();
            }
        }
        for (List<RangoBaremoBFA> factor : porFactor.values()) {
            factor.sort(java.util.Comparator.comparingInt(RangoBaremoBFA::limiteInferior));
            for (int i = 1; i < factor.size(); i++) {
                if (factor.get(i).limiteInferior() <= factor.get(i - 1).limiteSuperior()) {
                    throw new BaremoInvalidoException();
                }
            }
        }
    }

    public static class BaremoInvalidoException extends RuntimeException {
        public BaremoInvalidoException() {
            super("BAREMO_INVALIDO");
        }
    }

    public static class BaremoDuplicadoException extends IllegalStateException {
        public BaremoDuplicadoException() {
            super("Ya existe un baremo con el mismo nombre y versión");
        }
    }

    public record DatosBaremo(
            String nombre,
            String version,
            int anioNorma,
            String poblacionReferencia,
            String referenciaFuente,
            List<DatosRango> rangos) {}

    public record DatosRango(
            String idFactor,
            int percentil,
            Integer limiteInferior,
            Integer limiteSuperior,
            String estadoCelda,
            String valorOriginal,
            String localizadorFuente) {}
}
