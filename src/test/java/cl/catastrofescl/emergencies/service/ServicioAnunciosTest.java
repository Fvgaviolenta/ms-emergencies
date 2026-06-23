package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.request.ActualizarAnuncioRequest;
import cl.catastrofescl.emergencies.dto.request.PublicarAnuncioRequest;
import cl.catastrofescl.emergencies.dto.response.AnuncioResponse;
import cl.catastrofescl.emergencies.entity.AlcanceAnuncio;
import cl.catastrofescl.emergencies.entity.Anuncio;
import cl.catastrofescl.emergencies.entity.Emergencia;
import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import cl.catastrofescl.emergencies.entity.SeveridadAnuncio;
import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;
import cl.catastrofescl.emergencies.event.AnuncioPublicadoEvento;
import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.event.EventoDominio;
import cl.catastrofescl.emergencies.exception.AnuncioNoEncontradoException;
import cl.catastrofescl.emergencies.exception.EmergenciaNoActivaException;
import cl.catastrofescl.emergencies.exception.EmergenciaNoEncontradaException;
import cl.catastrofescl.emergencies.repository.RepositorioAnuncios;
import cl.catastrofescl.emergencies.repository.RepositorioEmergencias;
import cl.catastrofescl.emergencies.seguridad.ContextoUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAnunciosTest {

    @Mock private RepositorioAnuncios repositorioAnuncios;
    @Mock private RepositorioEmergencias repositorioEmergencias;
    @Mock private PublicadorEventos publicadorEventos;
    @Mock private ContextoUsuario contextoUsuario;

    @InjectMocks
    private ServicioAnuncios servicio;

    @Test
    void publicarAnuncioConEmergenciaActivaPersisteYEmiteEvento() {
        UUID emergenciaId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        when(repositorioEmergencias.findById(emergenciaId))
                .thenReturn(Optional.of(emergencia(emergenciaId, EstadoEmergencia.ACTIVA)));
        when(contextoUsuario.usuarioIdActual()).thenReturn(autorId);
        when(repositorioAnuncios.save(any(Anuncio.class))).thenAnswer(inv -> {
            Anuncio a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            a.setCreadoEn(OffsetDateTime.now());
            return a;
        });

        PublicarAnuncioRequest req = new PublicarAnuncioRequest(
                emergenciaId,
                "Evacuacion preventiva",
                "Se solicita evacuar zonas bajas",
                SeveridadAnuncio.URGENTE,
                AlcanceAnuncio.REGIONAL,
                "Valparaiso",
                null,
                null
        );

        AnuncioResponse resp = servicio.publicar(req);

        assertThat(resp.severidad()).isEqualTo(SeveridadAnuncio.URGENTE);
        assertThat(resp.vigenteDesde()).isNotNull();

        ArgumentCaptor<EventoDominio> captor = ArgumentCaptor.forClass(EventoDominio.class);
        org.mockito.Mockito.verify(publicadorEventos).publicar(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AnuncioPublicadoEvento.class);
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("announcement.published");
        AnuncioPublicadoEvento evento = (AnuncioPublicadoEvento) captor.getValue();
        assertThat(evento.getVersionEvento()).isEqualTo("1.0");
        assertThat(evento.getFuente()).isEqualTo("ms-emergencies");
        assertThat(evento.getTitulo()).isEqualTo("Evacuacion preventiva");
        assertThat(evento.getContenido()).isEqualTo("Se solicita evacuar zonas bajas");
    }

    @Test
    void publicarAnuncioEnEmergenciaFinalizadaLanzaConflicto() {
        UUID emergenciaId = UUID.randomUUID();
        when(repositorioEmergencias.findById(emergenciaId))
                .thenReturn(Optional.of(emergencia(emergenciaId, EstadoEmergencia.FINALIZADA)));

        PublicarAnuncioRequest req = new PublicarAnuncioRequest(
                emergenciaId, "Titulo", "Contenido",
                SeveridadAnuncio.INFORMATIVO, AlcanceAnuncio.NACIONAL,
                null, null, null
        );

        assertThatThrownBy(() -> servicio.publicar(req))
                .isInstanceOf(EmergenciaNoActivaException.class);
    }

    @Test
    void crearAnuncioAutomaticoDesdeEventoPersisteYEmiteAnnouncementPublished() {
        UUID emergenciaId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        when(repositorioEmergencias.findById(emergenciaId))
                .thenReturn(Optional.of(emergencia(emergenciaId, EstadoEmergencia.ACTIVA)));
        when(repositorioAnuncios.save(any(Anuncio.class))).thenAnswer(inv -> {
            Anuncio a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            a.setCreadoEn(OffsetDateTime.now());
            return a;
        });

        EmergenciaCreadaEvento evento = EmergenciaCreadaEvento.builder()
                .eventoId(UUID.randomUUID())
                .emergenciaId(emergenciaId)
                .tipo(TipoEmergencia.TERREMOTO)
                .severidad(SeveridadEmergencia.CATASTROFICA)
                .region("Metropolitana")
                .resumen("Emergencia TERREMOTO en Metropolitana")
                .declaradaPorUsuarioId(autorId)
                .ocurridoEn(OffsetDateTime.now())
                .build();

        UUID anuncioId = servicio.crearAnuncioAutomaticoDesdeEmergenciaCreada(evento);

        assertThat(anuncioId).isNotNull();

        ArgumentCaptor<Anuncio> captorAnuncio = ArgumentCaptor.forClass(Anuncio.class);
        org.mockito.Mockito.verify(repositorioAnuncios).save(captorAnuncio.capture());
        assertThat(captorAnuncio.getValue().getSeveridad()).isEqualTo(SeveridadAnuncio.EMERGENCIA);
        assertThat(captorAnuncio.getValue().getTitulo()).contains("TERREMOTO");

        ArgumentCaptor<EventoDominio> captorEvento = ArgumentCaptor.forClass(EventoDominio.class);
        org.mockito.Mockito.verify(publicadorEventos).publicar(captorEvento.capture());
        assertThat(captorEvento.getValue().getRoutingKey()).isEqualTo("announcement.published");
    }

    @Test
    void publicarAnuncioConEmergenciaInexistenteLanzaNotFound() {
        UUID emergenciaId = UUID.randomUUID();
        when(repositorioEmergencias.findById(emergenciaId)).thenReturn(Optional.empty());

        PublicarAnuncioRequest req = new PublicarAnuncioRequest(
                emergenciaId, "Titulo", "Contenido",
                SeveridadAnuncio.INFORMATIVO, AlcanceAnuncio.NACIONAL,
                null, null, null
        );

        assertThatThrownBy(() -> servicio.publicar(req))
                .isInstanceOf(EmergenciaNoEncontradaException.class);
    }

    @Test
    void actualizarAnuncioExistentePersisteCambios() {
        UUID anuncioId = UUID.randomUUID();
        Anuncio existente = Anuncio.builder()
                .id(anuncioId)
                .emergenciaId(UUID.randomUUID())
                .autorUsuarioId(UUID.randomUUID())
                .titulo("Titulo original")
                .contenido("Contenido original")
                .severidad(SeveridadAnuncio.INFORMATIVO)
                .alcance(AlcanceAnuncio.NACIONAL)
                .region("Metropolitana")
                .vigenteDesde(OffsetDateTime.now())
                .creadoEn(OffsetDateTime.now())
                .build();

        when(repositorioAnuncios.findById(anuncioId)).thenReturn(Optional.of(existente));
        when(repositorioAnuncios.save(any(Anuncio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAnuncioRequest req = new ActualizarAnuncioRequest(
                "Titulo actualizado",
                "Contenido actualizado",
                SeveridadAnuncio.URGENTE,
                AlcanceAnuncio.REGIONAL,
                "Valparaiso",
                OffsetDateTime.now().plusDays(7)
        );

        AnuncioResponse resp = servicio.actualizar(anuncioId, req);

        assertThat(resp.titulo()).isEqualTo("Titulo actualizado");
        assertThat(resp.contenido()).isEqualTo("Contenido actualizado");
        assertThat(resp.severidad()).isEqualTo(SeveridadAnuncio.URGENTE);
        assertThat(resp.alcance()).isEqualTo(AlcanceAnuncio.REGIONAL);
        assertThat(resp.region()).isEqualTo("Valparaiso");
        assertThat(resp.vigenteHasta()).isNotNull();
    }

    @Test
    void actualizarAnuncioInexistenteLanzaNotFound() {
        UUID anuncioId = UUID.randomUUID();
        when(repositorioAnuncios.findById(anuncioId)).thenReturn(Optional.empty());

        ActualizarAnuncioRequest req = new ActualizarAnuncioRequest(
                "Titulo", "Contenido",
                SeveridadAnuncio.INFORMATIVO, AlcanceAnuncio.NACIONAL,
                null, null
        );

        assertThatThrownBy(() -> servicio.actualizar(anuncioId, req))
                .isInstanceOf(AnuncioNoEncontradoException.class);
    }

    private Emergencia emergencia(UUID id, EstadoEmergencia estado) {
        return Emergencia.builder()
                .id(id)
                .tipo(TipoEmergencia.TERREMOTO)
                .severidad(SeveridadEmergencia.ALTA)
                .region("Metropolitana")
                .estado(estado)
                .declaradaPorUsuarioId(UUID.randomUUID())
                .declaradaEn(OffsetDateTime.now())
                .actualizadaEn(OffsetDateTime.now())
                .build();
    }
}
