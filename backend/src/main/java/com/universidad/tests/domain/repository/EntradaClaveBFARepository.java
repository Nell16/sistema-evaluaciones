package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import java.util.List;
import java.util.Optional;

public interface EntradaClaveBFARepository {

    Optional<EntradaClaveBFA> buscarPorId(Long idEntradaClave);

    List<EntradaClaveBFA> listarPorClave(Long idClave);

    Optional<EntradaClaveBFA> buscarPorClaveYPregunta(Long idClave, Long idPregunta);

    boolean existePorClaveYPregunta(Long idClave, Long idPregunta);

    long contarPorClave(Long idClave);

    EntradaClaveBFA guardar(EntradaClaveBFA entrada);
}
