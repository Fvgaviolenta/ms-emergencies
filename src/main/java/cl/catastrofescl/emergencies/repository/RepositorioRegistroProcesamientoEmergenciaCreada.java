package cl.catastrofescl.emergencies.repository;

import cl.catastrofescl.emergencies.entity.RegistroProcesamientoEmergenciaCreada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioRegistroProcesamientoEmergenciaCreada
        extends JpaRepository<RegistroProcesamientoEmergenciaCreada, UUID> {

    boolean existsByEventoId(UUID eventoId);

    Optional<RegistroProcesamientoEmergenciaCreada> findFirstByEmergenciaIdOrderByProcesadoEnDesc(UUID emergenciaId);
}
