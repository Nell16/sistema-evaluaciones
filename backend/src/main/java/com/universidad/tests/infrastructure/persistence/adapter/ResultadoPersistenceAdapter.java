package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.infrastructure.persistence.entity.ResultadoFactorJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.ResultadoJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.ResultadoFactorJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.ResultadoJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ResultadoPersistenceAdapter implements ResultadoRepository {

    private final ResultadoJpaRepository resultadoJpa;
    private final ResultadoFactorJpaRepository factorJpa;

    public ResultadoPersistenceAdapter(
            ResultadoJpaRepository resultadoJpa, ResultadoFactorJpaRepository factorJpa) {
        this.resultadoJpa = resultadoJpa;
        this.factorJpa = factorJpa;
    }

    @Override
    @Transactional
    public Resultado guardar(Resultado resultado) {
        ResultadoJpaEntity entidad =
                resultadoJpa
                        .findByIdEvaluacion(resultado.idEvaluacion())
                        .map(
                                existente -> {
                                    existente.actualizarCalculo(
                                            resultado.estadoResultado(),
                                            resultado.versionFormularioId());
                                    return resultadoJpa.saveAndFlush(existente);
                                })
                        .orElseGet(
                                () ->
                                        resultadoJpa.saveAndFlush(
                                                new ResultadoJpaEntity(
                                                        resultado.idEvaluacion(),
                                                        resultado.estadoResultado(),
                                                        resultado.versionFormularioId(),
                                                        resultado.creadoEn(),
                                                        resultado.referenciaRevisionVigente())));

        // Guardar o actualizar factores
        factorJpa.deleteAll(factorJpa.findByIdResultadoOrderByCodigoAsc(entidad.getIdResultado()));
        factorJpa.flush();
        List<ResultadoFactorJpaEntity> factoresGuardados =
                factorJpa.saveAllAndFlush(
                        resultado.factores().stream()
                                .map(
                                        f ->
                                                new ResultadoFactorJpaEntity(
                                                        entidad.getIdResultado(),
                                                        f.codigo(),
                                                        f.puntuacionDirecta(),
                                                        f.estadoCalculo(),
                                                        f.percentil(),
                                                        f.idRango(),
                                                        f.estadoNormativo(),
                                                        f.motivo()))
                                .toList());

        return aDominio(entidad, factoresGuardados);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Resultado> buscarPorEvaluacion(Long idEvaluacion) {
        return resultadoJpa
                .findByIdEvaluacion(idEvaluacion)
                .map(
                        r ->
                                aDominio(
                                        r,
                                        factorJpa.findByIdResultadoOrderByCodigoAsc(
                                                r.getIdResultado())));
    }

    private static Resultado aDominio(
            ResultadoJpaEntity entidad, List<ResultadoFactorJpaEntity> factores) {
        return new Resultado(
                entidad.getIdResultado(),
                entidad.getIdEvaluacion(),
                entidad.getEstado(),
                entidad.getVersionFormularioId(),
                entidad.getCreadoEn(),
                entidad.getReferenciaRevisionVigente(),
                factores.stream()
                        .map(
                                f ->
                                        new ResultadoFactor(
                                                f.getIdResultadoFactor(),
                                                f.getCodigo(),
                                                f.getPuntuacionDirecta(),
                                                f.getEstadoCalculo(),
                                                f.getPercentil(),
                                                f.getIdRango(),
                                                f.getEstadoNormativo(),
                                                f.getMotivo()))
                        .toList());
    }
}
