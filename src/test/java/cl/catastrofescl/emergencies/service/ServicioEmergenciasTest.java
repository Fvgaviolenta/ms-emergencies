package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.CoordenadaDto;
import cl.catastrofescl.emergencies.dto.request.SolicitudCentroAcopioEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.request.CambiarEstadoEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.request.DeclararEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.response.EmergenciaResponse;
import cl.catastrofescl.emergencies.entity.Emergencia;
import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;
import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.event.EstadoEmergenciaCambiadoEvento;
import cl.catastrofescl.emergencies.event.EventoDominio;
import cl.catastrofescl.emergencies.exception.EmergenciaNoEncontradaException;
import cl.catastrofescl.emergencies.exception.TransicionEstadoInvalidaException;
import cl.catastrofescl.emergencies.repository.ProyeccionPoligonoEmergenciaActiva;
import cl.catastrofescl.emergencies.repository.RepositorioEmergencias;
import cl.catastrofescl.emergencies.repository.RepositorioRegistroProcesamientoEmergenciaCreada;
import cl.catastrofescl.emergencies.seguridad.ContextoUsuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioEmergenciasTest {

    @Mock
    private RepositorioEmergencias repositorioEmergencias;

    @Mock
    private RepositorioRegistroProcesamientoEmergenciaCreada repositorioRegistroProcesamientoEmergenciaCreada;

    @Mock
    private PublicadorEventos publicadorEventos;

    @Mock
    private ContextoUsuario contextoUsuario;

    private GeometriaMapper geometriaMapper;
    private MapeadorEmergencias mapeadorEmergencias;

    private ServicioEmergencias servicio;

    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void init() {
        geometriaMapper = new GeometriaMapper();
        mapeadorEmergencias = new MapeadorEmergencias(geometriaMapper);
        servicio = new ServicioEmergencias(
                repositorioEmergencias,
                repositorioRegistroProcesamientoEmergenciaCreada,
                geometriaMapper,
                mapeadorEmergencias,
                publicadorEventos,
                contextoUsuario,
                new ObjectMapper()
        );
        when(repositorioRegistroProcesamientoEmergenciaCreada.findFirstByEmergenciaIdOrderByProcesadoEnDesc(any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void declararEmergenciaPersisteYPublicaEvento() {
        when(contextoUsuario.usuarioIdActual()).thenReturn(usuarioId);
        when(repositorioEmergencias.save(any(Emergencia.class))).thenAnswer(inv -> {
            Emergencia e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            e.setDeclaradaEn(OffsetDateTime.now());
            e.setActualizadaEn(OffsetDateTime.now());
            return e;
        });

        DeclararEmergenciaRequest solicitud = new DeclararEmergenciaRequest(
                TipoEmergencia.TERREMOTO,
                SeveridadEmergencia.ALTA,
                "Metropolitana",
                new CoordenadaDto(-70.65, -33.43),
                null,
                List.of()
        );

        EmergenciaResponse resp = servicio.declarar(solicitud);

        assertThat(resp).isNotNull();
        assertThat(resp.tipo()).isEqualTo(TipoEmergencia.TERREMOTO);
        assertThat(resp.estado()).isEqualTo(EstadoEmergencia.ACTIVA);
        assertThat(resp.coordenadasEpicentro()).isNotNull();

        ArgumentCaptor<EventoDominio> captor = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publicadorEventos, times(1)).publicar(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EmergenciaCreadaEvento.class);
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("emergency.created");
        EmergenciaCreadaEvento evento = (EmergenciaCreadaEvento) captor.getValue();
        assertThat(evento.getVersionEvento()).isEqualTo("1.0");
        assertThat(evento.getFuente()).isEqualTo("ms-emergencies");
        assertThat(evento.getEstado()).isEqualTo("ACTIVA");
        assertThat(evento.getResumen()).contains("TERREMOTO", "Metropolitana");
        assertThat(evento.getCentrosAsociados()).isEmpty();
    }

    @Test
    void declararConCentrosIncluyeCentrosEnEventoSinPersistirLocalmente() {
        when(contextoUsuario.usuarioIdActual()).thenReturn(usuarioId);
        when(repositorioEmergencias.save(any(Emergencia.class))).thenAnswer(inv -> {
            Emergencia e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            e.setDeclaradaEn(OffsetDateTime.now());
            e.setActualizadaEn(OffsetDateTime.now());
            return e;
        });

        DeclararEmergenciaRequest solicitud = new DeclararEmergenciaRequest(
                TipoEmergencia.INCENDIO,
                SeveridadEmergencia.MEDIA,
                "Valparaiso",
                new CoordenadaDto(-71.6, -33.0),
                null,
                List.of(new SolicitudCentroAcopioEmergenciaRequest(
                        "Centro Escuela Norte",
                        new CoordenadaDto(-71.61, -33.01),
                        500
                ))
        );

        servicio.declarar(solicitud);

        ArgumentCaptor<EventoDominio> captor = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publicadorEventos).publicar(captor.capture());
        EmergenciaCreadaEvento evento = (EmergenciaCreadaEvento) captor.getValue();
        assertThat(evento.getCentrosAsociados()).hasSize(1);
        assertThat(evento.getCentrosAsociados().get(0).getNombre()).isEqualTo("Centro Escuela Norte");
        assertThat(evento.getCentrosAsociados().get(0).getCapacidadEstimada()).isEqualTo(500);
    }

    @Test
    void listarGeoJsonActivasDevuelveFeatureCollectionMinimo() {
        UUID id = UUID.randomUUID();
        OffsetDateTime declaradaEn = OffsetDateTime.now();
        when(repositorioEmergencias.listarPoligonosActivosGeoJson())
                .thenReturn(List.of(new ProyeccionPoligono(
                        id,
                        "TERREMOTO",
                        "ALTA",
                        "Metropolitana",
                        "ACTIVA",
                        declaradaEn,
                        """
                                {"type":"Polygon","coordinates":[[[-70.7,-33.5],[-70.6,-33.5],[-70.6,-33.4],[-70.7,-33.5]]]}
                                """
                )));

        var respuesta = servicio.listarGeoJsonActivas();

        assertThat(respuesta.type()).isEqualTo("FeatureCollection");
        assertThat(respuesta.features()).hasSize(1);
        assertThat(respuesta.features().get(0).type()).isEqualTo("Feature");
        assertThat(respuesta.features().get(0).geometry().type()).isEqualTo("Polygon");
        assertThat(respuesta.features().get(0).properties().id()).isEqualTo(id);
        assertThat(respuesta.features().get(0).properties().estado()).isEqualTo("ACTIVA");
    }

    @Test
    void cambiarEstadoValidoPersisteYPublicaEvento() {
        UUID id = UUID.randomUUID();
        Emergencia existente = Emergencia.builder()
                .id(id)
                .tipo(TipoEmergencia.INCENDIO)
                .severidad(SeveridadEmergencia.MEDIA)
                .region("Valparaiso")
                .estado(EstadoEmergencia.ACTIVA)
                .declaradaPorUsuarioId(usuarioId)
                .declaradaEn(OffsetDateTime.now())
                .actualizadaEn(OffsetDateTime.now())
                .build();

        when(repositorioEmergencias.findById(id)).thenReturn(Optional.of(existente));
        when(repositorioEmergencias.save(any(Emergencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contextoUsuario.usuarioIdActual()).thenReturn(usuarioId);

        EmergenciaResponse resp = servicio.cambiarEstado(id,
                new CambiarEstadoEmergenciaRequest(EstadoEmergencia.CONTROLADA));

        assertThat(resp.estado()).isEqualTo(EstadoEmergencia.CONTROLADA);

        ArgumentCaptor<EventoDominio> captor = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publicadorEventos).publicar(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EstadoEmergenciaCambiadoEvento.class);
    }

    @Test
    void transicionInvalidaLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        Emergencia existente = Emergencia.builder()
                .id(id)
                .tipo(TipoEmergencia.TERREMOTO)
                .severidad(SeveridadEmergencia.CATASTROFICA)
                .region("Bio-Bio")
                .estado(EstadoEmergencia.FINALIZADA)
                .declaradaPorUsuarioId(usuarioId)
                .declaradaEn(OffsetDateTime.now())
                .actualizadaEn(OffsetDateTime.now())
                .build();

        when(repositorioEmergencias.findById(id)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicio.cambiarEstado(id,
                new CambiarEstadoEmergenciaRequest(EstadoEmergencia.ACTIVA)))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void emergenciaNoExisteLanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(repositorioEmergencias.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarEstado(id,
                new CambiarEstadoEmergenciaRequest(EstadoEmergencia.CONTROLADA)))
                .isInstanceOf(EmergenciaNoEncontradaException.class);
    }

    @Test
    void mismoEstadoNoPublicaEvento() {
        UUID id = UUID.randomUUID();
        Emergencia existente = Emergencia.builder()
                .id(id)
                .tipo(TipoEmergencia.INUNDACION)
                .severidad(SeveridadEmergencia.BAJA)
                .region("Coquimbo")
                .estado(EstadoEmergencia.ACTIVA)
                .declaradaPorUsuarioId(usuarioId)
                .declaradaEn(OffsetDateTime.now())
                .actualizadaEn(OffsetDateTime.now())
                .build();

        when(repositorioEmergencias.findById(id)).thenReturn(Optional.of(existente));

        servicio.cambiarEstado(id, new CambiarEstadoEmergenciaRequest(EstadoEmergencia.ACTIVA));

        verify(publicadorEventos, times(0)).publicar(any());
    }

    private record ProyeccionPoligono(
            UUID id,
            String tipo,
            String severidad,
            String region,
            String estado,
            OffsetDateTime declaradaEn,
            String zonaImpactoGeoJson
    ) implements ProyeccionPoligonoEmergenciaActiva {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getTipo() {
            return tipo;
        }

        @Override
        public String getSeveridad() {
            return severidad;
        }

        @Override
        public String getRegion() {
            return region;
        }

        @Override
        public String getEstado() {
            return estado;
        }

        @Override
        public OffsetDateTime getDeclaradaEn() {
            return declaradaEn;
        }

        @Override
        public String getZonaImpactoGeoJson() {
            return zonaImpactoGeoJson;
        }
    }
}
