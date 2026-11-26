package com.universidad.tests.application.service;

import static com.universidad.tests.domain.model.MatrizPermisos.OPERACION_AUTORIZAR;
import static com.universidad.tests.domain.model.MatrizPermisos.RECUPERAR_INTENTO;

import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ListadoEvaluacionesRepository;
import com.universidad.tests.dto.request.EstadoListadoEvaluacion;
import com.universidad.tests.dto.response.EvaluacionResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListadoEvaluacionesService {
    private final AutorizacionService autorizacion;
    private final ListadoEvaluacionesRepository listado;
    private final EvaluacionRepository evaluaciones;

    public ListadoEvaluacionesService(
            AutorizacionService autorizacion,
            ListadoEvaluacionesRepository listado,
            EvaluacionRepository evaluaciones) {
        this.autorizacion = autorizacion;
        this.listado = listado;
        this.evaluaciones = evaluaciones;
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listar(
            Long sujetoId, Long testId, Long periodoId, EstadoListadoEvaluacion estado) {
        var sesion = autorizacion.exigirSesion();
        if (!sesion.tienePermiso(RECUPERAR_INTENTO) && !sesion.tienePermiso(OPERACION_AUTORIZAR)) {
            throw new SinPermisoException();
        }

        Long cuentaRestringida = null;
        if ("SUJETO".equals(sesion.rolActivo())) {
            cuentaRestringida = sesion.idCuenta();
            if (cuentaRestringida == null) {
                throw new SinPermisoException();
            }
        }

        Instant ahora = Instant.now();
        return listado
                .buscarIds(
                        cuentaRestringida,
                        sujetoId,
                        testId,
                        periodoId,
                        estado == null ? null : EstadoEvaluacion.valueOf(estado.name()))
                .stream()
                .map(evaluaciones::buscarPorId)
                .flatMap(Optional::stream)
                .map(evaluacion -> EvaluacionResponseMapper.aRespuesta(evaluacion, ahora))
                .toList();
    }
}
