package cl.catastrofescl.emergencies.event;

import cl.catastrofescl.emergencies.entity.AlcanceAnuncio;
import cl.catastrofescl.emergencies.entity.SeveridadAnuncio;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnuncioPublicadoEvento implements EventoDominio {

    public static final String ROUTING_KEY = "announcement.published";

    private UUID eventoId;
    private OffsetDateTime ocurridoEn;
    private String correlacionId;
    private String versionEvento;
    private String fuente;

    private UUID anuncioId;
    private UUID emergenciaId;
    private UUID autorUsuarioId;
    private SeveridadAnuncio severidad;
    private AlcanceAnuncio alcance;
    private String region;
    private String titulo;
    private String contenido;
    private OffsetDateTime vigenteDesde;
    private OffsetDateTime vigenteHasta;

    @Override
    @JsonIgnore
    public String getRoutingKey() {
        return ROUTING_KEY;
    }
}
