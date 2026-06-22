package cl.catastrofescl.emergencies.exception;

import cl.catastrofescl.emergencies.entity.EstadoEmergencia;

import java.util.UUID;

public class EmergenciaNoActivaException extends RuntimeException {

    private final UUID emergenciaId;
    private final EstadoEmergencia estadoActual;

    public EmergenciaNoActivaException(UUID emergenciaId, EstadoEmergencia estadoActual) {
        super("La emergencia " + emergenciaId + " no esta en estado operativo (estado actual: " + estadoActual + ")");
        this.emergenciaId = emergenciaId;
        this.estadoActual = estadoActual;
    }

    public UUID getEmergenciaId() {
        return emergenciaId;
    }

    public EstadoEmergencia getEstadoActual() {
        return estadoActual;
    }
}
