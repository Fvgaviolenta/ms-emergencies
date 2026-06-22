package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.entity.RegistroProcesamientoEmergenciaCreada;
import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.repository.RepositorioRegistroProcesamientoEmergenciaCreada;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * Procesa el evento {@code emergency.created}: idempotencia, anuncio automatico para el portal
 * y registro de trazabilidad del consumo de cola.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioProcesamientoEmergenciaCreada {

    private static final String PREFIJO_REDIS = "processed:emergency.created:";

    private final RepositorioRegistroProcesamientoEmergenciaCreada repositorioRegistro;
    private final ServicioAnuncios servicioAnuncios;
    private final ObjectProvider<StringRedisTemplate> redisPlantilla;

    @Transactional
    public void procesar(EmergenciaCreadaEvento evento) {
        if (evento == null || evento.getEventoId() == null) {
            log.warn("Evento emergency.created sin eventoId, se ignora");
            return;
        }
        if (repositorioRegistro.existsByEventoId(evento.getEventoId())) {
            log.debug("Evento {} ya procesado (idempotencia BD)", evento.getEventoId());
            return;
        }

        try {
            repositorioRegistro.save(RegistroProcesamientoEmergenciaCreada.builder()
                    .eventoId(evento.getEventoId())
                    .emergenciaId(evento.getEmergenciaId())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            log.debug("Carrera al reservar procesamiento del evento {}: {}", evento.getEventoId(), ex.getMessage());
            return;
        }

        UUID anuncioId = servicioAnuncios.crearAnuncioAutomaticoDesdeEmergenciaCreada(evento);

        redisPlantilla.ifAvailable(redis -> {
            String clave = PREFIJO_REDIS + evento.getEventoId();
            redis.opsForValue().set(clave, "1", Duration.ofHours(24));
        });

        log.info("Cola: procesado emergency.created eventoId={} emergenciaId={} anuncioGeneradoId={}",
                evento.getEventoId(), evento.getEmergenciaId(), anuncioId);
    }
}
