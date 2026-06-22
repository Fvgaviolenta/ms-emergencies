package cl.catastrofescl.emergencies.exception;

import java.util.UUID;

public class EmergenciaNoEncontradaException extends RuntimeException {

    private final UUID emergenciaId;

    public EmergenciaNoEncontradaException(UUID emergenciaId) {
        super("No se encontro la emergencia con id " + emergenciaId);
        this.emergenciaId = emergenciaId;
    }

    public UUID getEmergenciaId() {
        return emergenciaId;
    }
}
