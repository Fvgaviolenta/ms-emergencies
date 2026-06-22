package cl.catastrofescl.emergencies.exception;

import cl.catastrofescl.emergencies.entity.EstadoEmergencia;

import java.util.UUID;

public class TransicionEstadoInvalidaException extends RuntimeException {

    private final UUID emergenciaId;
    private final EstadoEmergencia estadoActual;
    private final EstadoEmergencia estadoSolicitado;

    public TransicionEstadoInvalidaException(UUID emergenciaId,
                                             EstadoEmergencia estadoActual,
                                             EstadoEmergencia estadoSolicitado) {
        super("Transicion no permitida para la emergencia " + emergenciaId
                + ": " + estadoActual + " -> " + estadoSolicitado);
        this.emergenciaId = emergenciaId;
        this.estadoActual = estadoActual;
        this.estadoSolicitado = estadoSolicitado;
    }

    public UUID getEmergenciaId() {
        return emergenciaId;
    }

    public EstadoEmergencia getEstadoActual() {
        return estadoActual;
    }

    public EstadoEmergencia getEstadoSolicitado() {
        return estadoSolicitado;
    }
}
