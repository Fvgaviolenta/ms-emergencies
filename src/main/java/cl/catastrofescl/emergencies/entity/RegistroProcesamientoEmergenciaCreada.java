package cl.catastrofescl.emergencies.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evidencia de que el consumidor local proceso un mensaje {@code emergency.created}.
 */
@Entity
@Table(name = "registro_procesamiento_emergencia_creada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroProcesamientoEmergenciaCreada {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evento_id", nullable = false, unique = true)
    private UUID eventoId;

    @Column(name = "emergencia_id", nullable = false)
    private UUID emergenciaId;

    @Column(name = "procesado_en", nullable = false)
    private OffsetDateTime procesadoEn;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (procesadoEn == null) {
            procesadoEn = OffsetDateTime.now();
        }
    }
}
