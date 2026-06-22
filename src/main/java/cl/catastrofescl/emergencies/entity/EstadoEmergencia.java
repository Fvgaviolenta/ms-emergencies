package cl.catastrofescl.emergencies.entity;

import java.util.Set;

public enum EstadoEmergencia {
    ACTIVA,
    CONTROLADA,
    FINALIZADA;

    /**
     * Devuelve los estados hacia los que se puede transicionar desde este.
     * Regla de negocio: ACTIVA -> CONTROLADA -> FINALIZADA (solo hacia adelante).
     */
    public Set<EstadoEmergencia> transicionesPermitidas() {
        return switch (this) {
            case ACTIVA -> Set.of(CONTROLADA, FINALIZADA);
            case CONTROLADA -> Set.of(FINALIZADA);
            case FINALIZADA -> Set.of();
        };
    }

    public boolean puedeTransicionarA(EstadoEmergencia destino) {
        return transicionesPermitidas().contains(destino);
    }
}
