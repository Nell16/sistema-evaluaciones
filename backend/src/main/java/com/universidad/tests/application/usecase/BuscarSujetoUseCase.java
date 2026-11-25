package com.universidad.tests.application.usecase;

import com.universidad.tests.application.usecase.vista.SujetoVista;
import com.universidad.tests.domain.port.AutorizacionEvaluacionPort;
import com.universidad.tests.domain.repository.SujetoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** E23 — Buscar un Sujeto por identificador institucional; devuelve 0 o 1 elemento (CU-GEN-20). */
@Service
public class BuscarSujetoUseCase {

    private final AutorizacionEvaluacionPort autorizacion;
    private final SujetoRepository sujetoRepository;

    public BuscarSujetoUseCase(
            AutorizacionEvaluacionPort autorizacion, SujetoRepository sujetoRepository) {
        this.autorizacion = autorizacion;
        this.sujetoRepository = sujetoRepository;
    }

    @Transactional(readOnly = true)
    public List<SujetoVista> buscarPorIdentificador(String identificadorInstitucional) {
        autorizacion.exigirPermisoBuscarSujetos();
        if (identificadorInstitucional == null || identificadorInstitucional.isBlank()) {
            return List.of();
        }
        return sujetoRepository
                .buscarPorCodigoInstitucional(identificadorInstitucional.trim())
                .map(
                        s ->
                                List.of(
                                        new SujetoVista(
                                                s.idSujeto(),
                                                s.codigoInstitucional(),
                                                s.nombres(),
                                                s.apellidos())))
                .orElseGet(List::of);
    }
}
