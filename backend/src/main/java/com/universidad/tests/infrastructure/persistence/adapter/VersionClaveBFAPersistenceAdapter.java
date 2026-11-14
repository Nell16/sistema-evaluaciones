package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import com.universidad.tests.infrastructure.persistence.entity.VersionClaveBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.VersionClaveBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionFormularioJpaRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VersionClaveBFAPersistenceAdapter implements VersionClaveBFARepository {

    private final VersionClaveBFAJpaRepository claveJpaRepository;
    private final VersionFormularioJpaRepository formularioJpaRepository;

    public VersionClaveBFAPersistenceAdapter(
            VersionClaveBFAJpaRepository claveJpaRepository,
            VersionFormularioJpaRepository formularioJpaRepository) {

        this.claveJpaRepository = claveJpaRepository;
        this.formularioJpaRepository = formularioJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VersionClaveBFA> buscarPorId(Long idClave) {

        return claveJpaRepository.findById(idClave).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionClaveBFA> listarPorVersionFormulario(Long idVersionFormulario) {

        return claveJpaRepository
                .findByVersionFormulario_IdVersionFormularioOrderByVersionAsc(idVersionFormulario)
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VersionClaveBFA> buscarPorFormularioYVersion(
            Long idVersionFormulario, String version) {

        return claveJpaRepository
                .findByVersionFormulario_IdVersionFormularioAndVersion(idVersionFormulario, version)
                .map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorFormularioYVersion(Long idVersionFormulario, String version) {

        return claveJpaRepository.existsByVersionFormulario_IdVersionFormularioAndVersion(
                idVersionFormulario, version);
    }

    @Override
    @Transactional
    public VersionClaveBFA guardar(VersionClaveBFA clave) {

        if (clave == null) {
            throw new IllegalArgumentException("La clave de correccion es obligatoria");
        }

        // Solo permitir la creacion de claves nuevas.
        // La aprobacion y el retiro tendran operaciones protegidas.
        if (clave.getIdClave() != null) {
            throw new IllegalStateException("No se permite actualizar claves mediante este metodo");
        }

        if (clave.getEstado() != EstadoVersionClaveBFA.BORRADOR) {
            throw new IllegalArgumentException("Una clave nueva debe iniciar en BORRADOR");
        }

        // Comprobar que exista el formulario
        VersionFormularioJpaEntity formulario =
                formularioJpaRepository
                        .findById(clave.getIdVersionFormulario())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "No existe la version de formulario indicada"));

        // Impedir versiones duplicadas
        if (claveJpaRepository.existsByVersionFormulario_IdVersionFormularioAndVersion(
                clave.getIdVersionFormulario(), clave.getVersion())) {

            throw new IllegalArgumentException(
                    "Ya existe una clave con esa version para el formulario");
        }

        // Crear la entidad JPA
        VersionClaveBFAJpaEntity entidad =
                new VersionClaveBFAJpaEntity(
                        formulario, clave.getVersion(), clave.getReferenciaFuente());

        // Guardar y devolver el modelo de dominio
        return aDominio(claveJpaRepository.saveAndFlush(entidad));
    }

    private VersionClaveBFA aDominio(VersionClaveBFAJpaEntity entidad) {

        return VersionClaveBFA.restaurar(
                entidad.getIdClave(),
                entidad.getIdVersionFormulario(),
                entidad.getVersion(),
                entidad.getReferenciaFuente(),
                entidad.getEstado(),
                entidad.getEvidenciaAprobacion());
    }
}
