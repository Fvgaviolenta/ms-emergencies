package cl.catastrofescl.emergencies.exception;

import java.util.UUID;

public class AnuncioNoEncontradoException extends RuntimeException {

    private final UUID anuncioId;

    public AnuncioNoEncontradoException(UUID anuncioId) {
        super("No se encontro el anuncio con id " + anuncioId);
        this.anuncioId = anuncioId;
    }

    public UUID getAnuncioId() {
        return anuncioId;
    }
}
