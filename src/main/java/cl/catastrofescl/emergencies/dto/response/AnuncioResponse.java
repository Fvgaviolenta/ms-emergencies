package cl.catastrofescl.emergencies.dto.response;

import cl.catastrofescl.emergencies.entity.AlcanceAnuncio;
import cl.catastrofescl.emergencies.entity.SeveridadAnuncio;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnuncioResponse(
        UUID id,
        UUID emergenciaId,
        UUID autorUsuarioId,
        String titulo,
        String contenido,
        SeveridadAnuncio severidad,
        AlcanceAnuncio alcance,
        String region,
        OffsetDateTime vigenteDesde,
        OffsetDateTime vigenteHasta,
        OffsetDateTime creadoEn
) {
}
