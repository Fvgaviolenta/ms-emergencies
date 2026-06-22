package cl.catastrofescl.emergencies.dto.request;

import cl.catastrofescl.emergencies.entity.AlcanceAnuncio;
import cl.catastrofescl.emergencies.entity.SeveridadAnuncio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicarAnuncioRequest(
        @NotNull UUID emergenciaId,

        @NotBlank @Size(max = 200) String titulo,
        @NotBlank String contenido,

        @NotNull SeveridadAnuncio severidad,
        @NotNull AlcanceAnuncio alcance,

        @Size(max = 100) String region,

        OffsetDateTime vigenteDesde,
        OffsetDateTime vigenteHasta
) {
}
