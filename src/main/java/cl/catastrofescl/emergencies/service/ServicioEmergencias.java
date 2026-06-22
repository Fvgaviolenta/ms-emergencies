package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.request.CambiarEstadoEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.request.DeclararEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.request.SolicitudCentroAcopioEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.GeoJsonPoligonoDto;
import cl.catastrofescl.emergencies.dto.response.CaracteristicaGeoJsonEmergenciaResponse;
import cl.catastrofescl.emergencies.dto.response.ColeccionGeoJsonEmergenciasResponse;
import cl.catastrofescl.emergencies.dto.response.EmergenciaResponse;
import cl.catastrofescl.emergencies.dto.response.PropiedadesPoligonoEmergenciaResponse;
import cl.catastrofescl.emergencies.entity.Emergencia;
import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import cl.catastrofescl.emergencies.entity.RegistroProcesamientoEmergenciaCreada;
import cl.catastrofescl.emergencies.event.CentroAsociadoEnEventoDto;
import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.event.EstadoEmergenciaCambiadoEvento;
import cl.catastrofescl.emergencies.exception.EmergenciaNoEncontradaException;
import cl.catastrofescl.emergencies.exception.GeometriaInvalidaException;
import cl.catastrofescl.emergencies.exception.TransicionEstadoInvalidaException;
import cl.catastrofescl.emergencies.repository.ProyeccionPoligonoEmergenciaActiva;
import cl.catastrofescl.emergencies.repository.RepositorioEmergencias;
import cl.catastrofescl.emergencies.repository.RepositorioRegistroProcesamientoEmergenciaCreada;
import cl.catastrofescl.emergencies.seguridad.ContextoUsuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioEmergencias {

    private static final String VERSION_EVENTO = "1.0";
    private static final String FUENTE_EVENTO = "ms-emergencies";

    private final RepositorioEmergencias repositorioEmergencias;
    private final RepositorioRegistroProcesamientoEmergenciaCreada repositorioRegistroProcesamientoEmergenciaCreada;
    private final GeometriaMapper geometriaMapper;
    private final MapeadorEmergencias mapeadorEmergencias;
    private final PublicadorEventos publicadorEventos;
    private final ContextoUsuario contextoUsuario;
    private final ObjectMapper objectMapper;

    @Transactional
    public EmergenciaResponse declarar(DeclararEmergenciaRequest solicitud) {
        UUID usuarioId = contextoUsuario.usuarioIdActual();

        Emergencia emergencia = Emergencia.builder()
                .tipo(solicitud.tipo())
                .severidad(solicitud.severidad())
                .region(solicitud.region())
                .estado(EstadoEmergencia.ACTIVA)
                .coordenadasEpicentro(geometriaMapper.aPunto(solicitud.epicentro()))
                .zonaImpacto(geometriaMapper.aPoligono(solicitud.zonaImpacto()))
                .declaradaPorUsuarioId(usuarioId)
                .build();

        Emergencia persistida = repositorioEmergencias.save(emergencia);
        log.info("Emergencia declarada id={} tipo={} severidad={} region={}",
                persistida.getId(), persistida.getTipo(), persistida.getSeveridad(), persistida.getRegion());

        List<CentroAsociadoEnEventoDto> centrosEnEvento = solicitud.centrosAcopio().stream()
                .map(this::aCentroEvento)
                .toList();

        UUID eventoId = UUID.randomUUID();
        EmergenciaCreadaEvento evento = EmergenciaCreadaEvento.builder()
                .eventoId(eventoId)
                .ocurridoEn(OffsetDateTime.now())
                .correlacionId(persistida.getId().toString())
                .versionEvento(VERSION_EVENTO)
                .fuente(FUENTE_EVENTO)
                .emergenciaId(persistida.getId())
                .tipo(persistida.getTipo())
                .severidad(persistida.getSeveridad())
                .estado(persistida.getEstado().name())
                .region(persistida.getRegion())
                .resumen(construirResumenEmergencia(persistida))
                .declaradaPorUsuarioId(usuarioId)
                .centrosAsociados(centrosEnEvento)
                .build();

        publicarEmergenciaCreadaTrasCommit(evento);

        return aRespuestaDetalle(persistida);
    }

    private void publicarEmergenciaCreadaTrasCommit(EmergenciaCreadaEvento evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicadorEventos.publicar(evento);
                }
            });
        } else {
            publicadorEventos.publicar(evento);
        }
    }

    private CentroAsociadoEnEventoDto aCentroEvento(SolicitudCentroAcopioEmergenciaRequest solicitudCentro) {
        return CentroAsociadoEnEventoDto.builder()
                .nombre(solicitudCentro.nombre())
                .longitud(solicitudCentro.ubicacion().longitud())
                .latitud(solicitudCentro.ubicacion().latitud())
                .capacidadEstimada(solicitudCentro.capacidadEstimada())
                .build();
    }

    @Transactional
    public EmergenciaResponse cambiarEstado(UUID emergenciaId, CambiarEstadoEmergenciaRequest solicitud) {
        Emergencia emergencia = repositorioEmergencias.findById(emergenciaId)
                .orElseThrow(() -> new EmergenciaNoEncontradaException(emergenciaId));

        EstadoEmergencia estadoActual = emergencia.getEstado();
        EstadoEmergencia estadoNuevo = solicitud.nuevoEstado();

        if (estadoActual == estadoNuevo) {
            log.debug("Emergencia {} ya esta en estado {}, no se realiza cambio", emergenciaId, estadoNuevo);
            return aRespuestaDetalle(emergencia);
        }

        if (!estadoActual.puedeTransicionarA(estadoNuevo)) {
            throw new TransicionEstadoInvalidaException(emergenciaId, estadoActual, estadoNuevo);
        }

        emergencia.setEstado(estadoNuevo);
        Emergencia actualizada = repositorioEmergencias.save(emergencia);

        UUID usuarioId = contextoUsuario.usuarioIdActual();
        log.info("Emergencia {} transiciona {} -> {} por usuario {}",
                emergenciaId, estadoActual, estadoNuevo, usuarioId);

        publicadorEventos.publicar(EstadoEmergenciaCambiadoEvento.builder()
                .eventoId(UUID.randomUUID())
                .ocurridoEn(OffsetDateTime.now())
                .correlacionId(emergenciaId.toString())
                .emergenciaId(emergenciaId)
                .estadoAnterior(estadoActual)
                .estadoNuevo(estadoNuevo)
                .actualizadaPorUsuarioId(usuarioId)
                .build());

        return aRespuestaDetalle(actualizada);
    }

    @Transactional(readOnly = true)
    public EmergenciaResponse obtener(UUID emergenciaId) {
        Emergencia emergencia = repositorioEmergencias.findById(emergenciaId)
                .orElseThrow(() -> new EmergenciaNoEncontradaException(emergenciaId));
        return aRespuestaDetalle(emergencia);
    }

    @Transactional(readOnly = true)
    public List<EmergenciaResponse> listarActivas() {
        return repositorioEmergencias
                .findByEstadoOrderByDeclaradaEnDesc(EstadoEmergencia.ACTIVA)
                .stream()
                .map(this::aRespuestaDetalle)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColeccionGeoJsonEmergenciasResponse listarGeoJsonActivas() {
        List<CaracteristicaGeoJsonEmergenciaResponse> caracteristicas = repositorioEmergencias
                .listarPoligonosActivosGeoJson()
                .stream()
                .map(this::aCaracteristicaGeoJson)
                .toList();

        return ColeccionGeoJsonEmergenciasResponse.de(caracteristicas);
    }

    private EmergenciaResponse aRespuestaDetalle(Emergencia emergencia) {
        OffsetDateTime procesadoEn = repositorioRegistroProcesamientoEmergenciaCreada
                .findFirstByEmergenciaIdOrderByProcesadoEnDesc(emergencia.getId())
                .map(RegistroProcesamientoEmergenciaCreada::getProcesadoEn)
                .orElse(null);
        return mapeadorEmergencias.aResponse(emergencia, procesadoEn);
    }

    private CaracteristicaGeoJsonEmergenciaResponse aCaracteristicaGeoJson(
            ProyeccionPoligonoEmergenciaActiva proyeccion) {
        PropiedadesPoligonoEmergenciaResponse propiedades = new PropiedadesPoligonoEmergenciaResponse(
                proyeccion.getId(),
                proyeccion.getTipo(),
                proyeccion.getSeveridad(),
                proyeccion.getRegion(),
                proyeccion.getEstado(),
                proyeccion.getDeclaradaEn()
        );

        return CaracteristicaGeoJsonEmergenciaResponse.de(
                leerPoligonoGeoJson(proyeccion.getZonaImpactoGeoJson()),
                propiedades
        );
    }

    private GeoJsonPoligonoDto leerPoligonoGeoJson(String geoJson) {
        try {
            return objectMapper.readValue(geoJson, GeoJsonPoligonoDto.class);
        } catch (JsonProcessingException ex) {
            throw new GeometriaInvalidaException("No se pudo leer el poligono GeoJSON almacenado");
        }
    }

    private String construirResumenEmergencia(Emergencia emergencia) {
        return "Emergencia " + emergencia.getTipo()
                + " de severidad " + emergencia.getSeveridad()
                + " declarada en " + emergencia.getRegion();
    }
}
