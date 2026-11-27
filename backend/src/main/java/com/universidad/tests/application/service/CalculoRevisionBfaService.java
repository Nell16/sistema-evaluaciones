package com.universidad.tests.application.service;

import com.universidad.tests.application.evaluator.bfa.BfaTestEvaluator;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.port.PreparacionRevisionBfaPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CalculoRevisionBfaService implements PreparacionRevisionBfaPort {
    private final CalculoPuntuacionesBfaService puntuaciones;
    private final AplicarBaremoBFAService baremos;

    public CalculoRevisionBfaService(
            CalculoPuntuacionesBfaService puntuaciones, AplicarBaremoBFAService baremos) {
        this.puntuaciones = puntuaciones;
        this.baremos = baremos;
    }

    public Calculo preparar(
            Long evaluacion, Long version, String reglas, Long clavePedida, Long baremoPedido) {
        var entradas =
                puntuaciones
                        .calcular(evaluacion, version)
                        .orElseThrow(() -> invalida("La evaluación no pertenece a BFA."));
        if (clavePedida != null && !Objects.equals(clavePedida, entradas.idClave()))
            throw invalida("La clave solicitada no es la clave aprobada aplicada al cálculo.");
        Map<String, Integer> insumos = new LinkedHashMap<>();
        for (String codigo :
                List.of("VOC1", "VOC2", "CV", "S1", "S2", "R1", "R2", "N1", "N2", "FV"))
            insumos.put(codigo, entradas.puntuaciones().get(codigo));
        var calculados = new BfaTestEvaluator().consolidar(evaluacion, version, insumos).factores();
        Map<String, Integer> directas = new LinkedHashMap<>();
        calculados.forEach((codigo, factor) -> directas.put(codigo, factor.puntuacionDirecta()));
        var norma = baremos.aplicarRevision(version, reglas, directas);
        if (baremoPedido != null && !Objects.equals(baremoPedido, norma.idBaremo()))
            throw invalida("El baremo solicitado no es el único aprobado y compatible.");
        var factores =
                calculados.values().stream()
                        .map(
                                f -> {
                                    var n = norma.factores().get(f.codigo());
                                    return new ResultadoFactor(
                                            null,
                                            f.codigo(),
                                            f.puntuacionDirecta(),
                                            f.puntuacionDirecta() == null
                                                    ? "NO_CALCULABLE"
                                                    : "CALCULADO",
                                            n.percentil(),
                                            n.idRango(),
                                            n.estadoNormativo().name(),
                                            f.motivo() != null ? f.motivo() : n.motivo());
                                })
                        .toList();
        return new Calculo(entradas.idClave(), norma.idBaremo(), factores);
    }

    private static RevisionBfaException invalida(String mensaje) {
        return new RevisionBfaException(RevisionBfaException.Codigo.SOLICITUD_INVALIDA, mensaje);
    }
}
