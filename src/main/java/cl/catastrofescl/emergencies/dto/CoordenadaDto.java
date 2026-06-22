package cl.catastrofescl.emergencies.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Par de coordenadas (longitud, latitud) en SRID 4326.
 * Se recibe como [lng, lat] siguiendo la convencion de GeoJSON.
 */
public record CoordenadaDto(
        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitud,

        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitud
) {
}
