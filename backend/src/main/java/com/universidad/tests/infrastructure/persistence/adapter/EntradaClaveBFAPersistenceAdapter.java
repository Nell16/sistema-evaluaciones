package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.infrastructure.persistence.entity.EntradaClaveBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.OpcionRespuestaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.PreguntaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionClaveBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.EntradaClaveBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.OpcionRespuestaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.PreguntaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionClaveBFAJpaRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EntradaClaveBFAPersistenceAdapter implements EntradaClaveBFARepository {

    private final EntradaClaveBFAJpaRepository entradaJpaRepository;
    private final VersionClaveBFAJpaRepository claveJpaRepository;
    private final PreguntaJpaRepository preguntaJpaRepository;
    private final OpcionRespuestaJpaRepository opcionJpaRepository;

    public EntradaClaveBFAPersistenceAdapter(
            EntradaClaveBFAJpaRepository entradaJpaRepository,
            VersionClaveBFAJpaRepository claveJpaRepository,
            PreguntaJpaRepository preguntaJpaRepository,
            OpcionRespuestaJpaRepository opcionJpaRepository) {

        this.entradaJpaRepository = entradaJpaRepository;
        this.claveJpaRepository = claveJpaRepository;
        this.preguntaJpaRepository = preguntaJpaRepository;
        this.opcionJpaRepository = opcionJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntradaClaveBFA> buscarPorId(Long idEntradaClave) {

        return entradaJpaRepository.findById(idEntradaClave).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntradaClaveBFA> listarPorClave(Long idClave) {

        return entradaJpaRepository.findByClave_IdClave(idClave).stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntradaClaveBFA> buscarPorClaveYPregunta(Long idClave, Long idPregunta) {

        return entradaJpaRepository
                .findByClave_IdClaveAndPregunta_IdPregunta(idClave, idPregunta)
                .map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorClaveYPregunta(Long idClave, Long idPregunta) {

        return entradaJpaRepository.existsByClave_IdClaveAndPregunta_IdPregunta(
                idClave, idPregunta);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPorClave(Long idClave) {

        return entradaJpaRepository.countByClave_IdClave(idClave);
    }

    @Override
    @Transactional
    public EntradaClaveBFA guardar(EntradaClaveBFA entrada) {

        if (entrada == null) {
            throw new IllegalArgumentException("La entrada de correccion es obligatoria");
        }

        // Solo permitir entradas nuevas
        if (entrada.getIdEntradaClave() != null) {
            throw new IllegalStateException(
                    "No se permite actualizar entradas mediante este metodo");
        }

        // Recuperar la version de clave
        VersionClaveBFAJpaEntity clave =
                claveJpaRepository
                        .findById(entrada.getIdClave())
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la clave indicada"));

        // Solo permitir agregar entradas a claves en borrador
        if (clave.getEstado() != EstadoVersionClaveBFA.BORRADOR) {
            throw new IllegalStateException("Solo se pueden agregar entradas a claves en BORRADOR");
        }

        // Recuperar la pregunta
        PreguntaJpaEntity pregunta =
                preguntaJpaRepository
                        .findById(entrada.getIdPregunta())
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la pregunta indicada"));

        // Verificar que la pregunta pertenezca al formulario
        Long idFormularioPregunta =
                pregunta.getConfiguracionSubprueba()
                        .getVersionFormulario()
                        .getIdVersionFormulario();

        if (!Objects.equals(idFormularioPregunta, clave.getIdVersionFormulario())) {

            throw new IllegalArgumentException(
                    "La pregunta no pertenece al formulario de la clave");
        }

        // Recuperar la opcion correcta
        OpcionRespuestaJpaEntity opcion =
                opcionJpaRepository
                        .findById(entrada.getIdOpcionCorrecta())
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la opcion indicada"));

        // Verificar que la opcion pertenezca a la pregunta
        if (!Objects.equals(opcion.getPregunta().getIdPregunta(), pregunta.getIdPregunta())) {

            throw new IllegalArgumentException("La opcion correcta no pertenece a la pregunta");
        }

        // Evitar entradas duplicadas
        if (entradaJpaRepository.existsByClave_IdClaveAndPregunta_IdPregunta(
                clave.getIdClave(), pregunta.getIdPregunta())) {

            throw new IllegalArgumentException("La pregunta ya tiene una entrada en esta clave");
        }

        // Crear y almacenar la entidad
        EntradaClaveBFAJpaEntity entidad = new EntradaClaveBFAJpaEntity(clave, pregunta, opcion);

        return aDominio(entradaJpaRepository.saveAndFlush(entidad));
    }

    private EntradaClaveBFA aDominio(EntradaClaveBFAJpaEntity entidad) {

        return new EntradaClaveBFA(
                entidad.getIdEntradaClave(),
                entidad.getClave().getIdClave(),
                entidad.getPregunta().getIdPregunta(),
                entidad.getOpcionCorrecta().getIdOpcion());
    }
}
