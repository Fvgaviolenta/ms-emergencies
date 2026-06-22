package cl.catastrofescl.emergencies.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProyeccionPoligonoEmergenciaActiva {
    UUID getId();

    String getTipo();

    String getSeveridad();

    String getRegion();

    String getEstado();

    OffsetDateTime getDeclaradaEn();

    String getZonaImpactoGeoJson();
}
