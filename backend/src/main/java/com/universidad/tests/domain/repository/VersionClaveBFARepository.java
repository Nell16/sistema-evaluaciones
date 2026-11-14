package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.VersionClaveBFA;
import java.util.List;
import java.util.Optional;

public interface VersionClaveBFARepository {

    Optional<VersionClaveBFA> buscarPorId(Long idClave);

    List<VersionClaveBFA> listarPorVersionFormulario(Long idVersionFormulario);

    Optional<VersionClaveBFA> buscarPorFormularioYVersion(Long idVersionFormulario, String version);

    boolean existePorFormularioYVersion(Long idVersionFormulario, String version);

    VersionClaveBFA guardar(VersionClaveBFA clave);
}
