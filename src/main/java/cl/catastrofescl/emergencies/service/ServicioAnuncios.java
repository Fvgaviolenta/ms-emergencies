package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.request.ActualizarAnuncioRequest;
import cl.catastrofescl.emergencies.dto.request.PublicarAnuncioRequest;
import cl.catastrofescl.emergencies.dto.response.AnuncioResponse;
import cl.catastrofescl.emergencies.entity.AlcanceAnuncio;
import cl.catastrofescl.emergencies.entity.Anuncio;
import cl.catastrofescl.emergencies.entity.Emergencia;
import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import cl.catastrofescl.emergencies.entity.SeveridadAnuncio;
import cl.catastrofescl.emergencies.event.AnuncioPublicadoEvento;
import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;
import cl.catastrofescl.emergencies.exception.AnuncioNoEncontradoException;
import cl.catastrofescl.emergencies.exception.EmergenciaNoActivaException;
import cl.catastrofescl.emergencies.exception.EmergenciaNoEncontradaException;
import cl.catastrofescl.emergencies.repository.RepositorioAnuncios;
import cl.catastrofescl.emergencies.repository.RepositorioEmergencias;
import cl.catastrofescl.emergencies.seguridad.ContextoUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioAnuncios {

    private static final String VERSION_EVENTO = "1.0";
    private static final String FUENTE_EVENTO = "ms-emergencies";

    private final RepositorioAnuncios repositorioAnuncios;
    private final RepositorioEmergencias repositorioEmergencias;
    private final PublicadorEventos publicadorEventos;
    private final ContextoUsuario contextoUsuario;

    @Transactional
    public AnuncioResponse publicar(PublicarAnuncioRequest solicitud) {
        Emergencia emergencia = repositorioEmergencias.findById(solicitud.emergenciaId())
                .orElseThrow(() -> new EmergenciaNoEncontradaException(solicitud.emergenciaId()));

        if (emergencia.getEstado() == EstadoEmergencia.FINALIZADA) {
            throw new EmergenciaNoActivaException(emergencia.getId(), emergencia.getEstado());
        }

        UUID autorId = contextoUsuario.usuarioIdActual();
        OffsetDateTime vigenteDesde = solicitud.vigenteDesde() != null
                ? solicitud.vigenteDesde()
                : OffsetDateTime.now();

        Anuncio anuncio = Anuncio.builder()
                .emergenciaId(solicitud.emergenciaId())
                .autorUsuarioId(autorId)
                .titulo(solicitud.titulo())
                .contenido(solicitud.contenido())
                .severidad(solicitud.severidad())
                .alcance(solicitud.alcance())
                .region(solicitud.region())
                .vigenteDesde(vigenteDesde)
                .vigenteHasta(solicitud.vigenteHasta())
                .build();

        Anuncio persistido = repositorioAnuncios.save(anuncio);
        log.info("Anuncio publicado id={} severidad={} alcance={} emergencia={}",
                persistido.getId(), persistido.getSeveridad(), persistido.getAlcance(), persistido.getEmergenciaId());

        publicarEventoAnuncio(persistido);
        return aResponse(persistido);
    }

    /**
     * Crea un anuncio de alerta a partir del evento {@code emergency.created} consumido por cola.
     * El frontend puede mostrarlo con {@code GET /announcements} (publico, paginado).
     */
    @Transactional
    public UUID crearAnuncioAutomaticoDesdeEmergenciaCreada(EmergenciaCreadaEvento evento) {
        Emergencia emergencia = repositorioEmergencias.findById(evento.getEmergenciaId())
                .orElseThrow(() -> new EmergenciaNoEncontradaException(evento.getEmergenciaId()));

        if (emergencia.getEstado() == EstadoEmergencia.FINALIZADA) {
            throw new EmergenciaNoActivaException(emergencia.getId(), emergencia.getEstado());
        }

        UUID autorId = evento.getDeclaradaPorUsuarioId() != null
                ? evento.getDeclaradaPorUsuarioId()
                : emergencia.getDeclaradaPorUsuarioId();

        OffsetDateTime vigenteDesde = evento.getOcurridoEn() != null
                ? evento.getOcurridoEn()
                : OffsetDateTime.now();

        String titulo = construirTituloAnuncioAutomatico(evento);
        String contenido = construirContenidoAnuncioAutomatico(evento);

        Anuncio anuncio = Anuncio.builder()
                .emergenciaId(evento.getEmergenciaId())
                .autorUsuarioId(autorId)
                .titulo(titulo)
                .contenido(contenido)
                .severidad(mapearSeveridadAnuncio(evento.getSeveridad()))
                .alcance(AlcanceAnuncio.REGIONAL)
                .region(evento.getRegion())
                .vigenteDesde(vigenteDesde)
                .vigenteHasta(null)
                .build();

        Anuncio persistido = repositorioAnuncios.save(anuncio);
        log.info("Anuncio automatico por cola id={} emergencia={} severidad={}",
                persistido.getId(), persistido.getEmergenciaId(), persistido.getSeveridad());

        publicarEventoAnuncio(persistido);
        return persistido.getId();
    }

    /**
     * El repositorio fija ORDER BY en JPQL (severidad, vigenteDesde). El {@link Pageable} del cliente
     * solo aplica tamaño/pagina: se ignora cualquier {@code sort=...} para evitar errores Hibernate
     * (p. ej. {@code sort=string} desde Swagger/UI genera orden por {@code a.string} inexistente).
     */
    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarVigentes(Pageable pageable) {
        Pageable soloPagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return repositorioAnuncios
                .listarVigentes(OffsetDateTime.now(), soloPagina)
                .map(this::aResponse);
    }

    @Transactional
    public AnuncioResponse actualizar(UUID anuncioId, ActualizarAnuncioRequest solicitud) {
        Anuncio anuncio = repositorioAnuncios.findById(anuncioId)
                .orElseThrow(() -> new AnuncioNoEncontradoException(anuncioId));

        anuncio.setTitulo(solicitud.titulo());
        anuncio.setContenido(solicitud.contenido());
        anuncio.setSeveridad(solicitud.severidad());
        anuncio.setAlcance(solicitud.alcance());
        anuncio.setRegion(solicitud.region());
        anuncio.setVigenteHasta(solicitud.vigenteHasta());

        Anuncio persistido = repositorioAnuncios.save(anuncio);
        log.info("Anuncio actualizado id={} severidad={} alcance={}",
                persistido.getId(), persistido.getSeveridad(), persistido.getAlcance());
        return aResponse(persistido);
    }

    private void publicarEventoAnuncio(Anuncio persistido) {
        publicadorEventos.publicar(AnuncioPublicadoEvento.builder()
                .eventoId(UUID.randomUUID())
                .ocurridoEn(OffsetDateTime.now())
                .correlacionId(persistido.getId().toString())
                .versionEvento(VERSION_EVENTO)
                .fuente(FUENTE_EVENTO)
                .anuncioId(persistido.getId())
                .emergenciaId(persistido.getEmergenciaId())
                .autorUsuarioId(persistido.getAutorUsuarioId())
                .severidad(persistido.getSeveridad())
                .alcance(persistido.getAlcance())
                .region(persistido.getRegion())
                .titulo(persistido.getTitulo())
                .contenido(persistido.getContenido())
                .vigenteDesde(persistido.getVigenteDesde())
                .vigenteHasta(persistido.getVigenteHasta())
                .build());
    }

    private static String construirTituloAnuncioAutomatico(EmergenciaCreadaEvento evento) {
        TipoEmergencia tipo = evento.getTipo() != null ? evento.getTipo() : TipoEmergencia.TERREMOTO;
        String region = evento.getRegion() != null ? evento.getRegion() : "Chile";
        return "Emergencia activa: " + tipo + " en " + region;
    }

    private static String construirContenidoAnuncioAutomatico(EmergenciaCreadaEvento evento) {
        StringBuilder sb = new StringBuilder();
        if (evento.getResumen() != null && !evento.getResumen().isBlank()) {
            sb.append(evento.getResumen().trim());
        } else {
            sb.append(construirTituloAnuncioAutomatico(evento));
        }
        List<?> centros = evento.getCentrosAsociados();
        if (centros != null && !centros.isEmpty()) {
            sb.append(" Se registraron ").append(centros.size()).append(" centro(s) de acopio asociados.");
        }
        sb.append(" Consulte el mapa de emergencias activas para mas detalle.");
        return sb.toString();
    }

    private static SeveridadAnuncio mapearSeveridadAnuncio(SeveridadEmergencia severidadEmergencia) {
        if (severidadEmergencia == null) {
            return SeveridadAnuncio.IMPORTANTE;
        }
        return switch (severidadEmergencia) {
            case CATASTROFICA -> SeveridadAnuncio.EMERGENCIA;
            case ALTA -> SeveridadAnuncio.URGENTE;
            case MEDIA -> SeveridadAnuncio.IMPORTANTE;
            case BAJA -> SeveridadAnuncio.INFORMATIVO;
        };
    }

    private AnuncioResponse aResponse(Anuncio anuncio) {
        return new AnuncioResponse(
                anuncio.getId(),
                anuncio.getEmergenciaId(),
                anuncio.getAutorUsuarioId(),
                anuncio.getTitulo(),
                anuncio.getContenido(),
                anuncio.getSeveridad(),
                anuncio.getAlcance(),
                anuncio.getRegion(),
                anuncio.getVigenteDesde(),
                anuncio.getVigenteHasta(),
                anuncio.getCreadoEn()
        );
    }
}
