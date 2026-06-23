package cl.catastrofescl.emergencies.repository;

import java.time.Instant;
import java.util.UUID;

public interface ProyeccionPoligonoEmergenciaActiva {
    UUID getId();

    String getTipo();

    String getSeveridad();

    String getRegion();

    String getEstado();

    // La consulta nativa lee la columna TIMESTAMPTZ como java.time.Instant.
    // Se expone como Instant y el servicio lo convierte a OffsetDateTime para el DTO.
    Instant getDeclaradaEn();

    String getZonaImpactoGeoJson();
}
