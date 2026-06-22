package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.entity.RegistroProcesamientoEmergenciaCreada;
import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;
import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.repository.RepositorioRegistroProcesamientoEmergenciaCreada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioProcesamientoEmergenciaCreadaTest {

    @Mock
    private RepositorioRegistroProcesamientoEmergenciaCreada repositorioRegistro;

    @Mock
    private ServicioAnuncios servicioAnuncios;

    @Mock
    private ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisPlantilla;

    @InjectMocks
    private ServicioProcesamientoEmergenciaCreada servicio;

    @Test
    void procesarEventoNuevoGeneraAnuncioYRegistro() {
        UUID eventoId = UUID.randomUUID();
        UUID emergenciaId = UUID.randomUUID();
        EmergenciaCreadaEvento evento = EmergenciaCreadaEvento.builder()
                .eventoId(eventoId)
                .emergenciaId(emergenciaId)
                .tipo(TipoEmergencia.INCENDIO)
                .severidad(SeveridadEmergencia.ALTA)
                .region("Valparaiso")
                .resumen("Emergencia INCENDIO en Valparaiso")
                .ocurridoEn(OffsetDateTime.now())
                .build();

        when(repositorioRegistro.existsByEventoId(eventoId)).thenReturn(false);
        when(repositorioRegistro.save(any(RegistroProcesamientoEmergenciaCreada.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(servicioAnuncios.crearAnuncioAutomaticoDesdeEmergenciaCreada(evento))
                .thenReturn(UUID.randomUUID());

        servicio.procesar(evento);

        verify(servicioAnuncios).crearAnuncioAutomaticoDesdeEmergenciaCreada(evento);
        verify(repositorioRegistro).save(any(RegistroProcesamientoEmergenciaCreada.class));
    }

    @Test
    void procesarEventoDuplicadoNoGeneraAnuncio() {
        UUID eventoId = UUID.randomUUID();
        EmergenciaCreadaEvento evento = EmergenciaCreadaEvento.builder()
                .eventoId(eventoId)
                .emergenciaId(UUID.randomUUID())
                .build();

        when(repositorioRegistro.existsByEventoId(eventoId)).thenReturn(true);

        servicio.procesar(evento);

        verify(servicioAnuncios, never()).crearAnuncioAutomaticoDesdeEmergenciaCreada(any());
        verify(repositorioRegistro, never()).save(any());
    }
}
