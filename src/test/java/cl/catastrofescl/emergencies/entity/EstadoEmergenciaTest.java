package cl.catastrofescl.emergencies.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoEmergenciaTest {

    @Test
    void activaPuedeIrAControladaOFinalizada() {
        assertThat(EstadoEmergencia.ACTIVA.puedeTransicionarA(EstadoEmergencia.CONTROLADA)).isTrue();
        assertThat(EstadoEmergencia.ACTIVA.puedeTransicionarA(EstadoEmergencia.FINALIZADA)).isTrue();
    }

    @Test
    void controladaSoloPuedeIrAFinalizada() {
        assertThat(EstadoEmergencia.CONTROLADA.puedeTransicionarA(EstadoEmergencia.FINALIZADA)).isTrue();
        assertThat(EstadoEmergencia.CONTROLADA.puedeTransicionarA(EstadoEmergencia.ACTIVA)).isFalse();
    }

    @Test
    void finalizadaEsEstadoTerminal() {
        assertThat(EstadoEmergencia.FINALIZADA.transicionesPermitidas()).isEmpty();
        assertThat(EstadoEmergencia.FINALIZADA.puedeTransicionarA(EstadoEmergencia.ACTIVA)).isFalse();
        assertThat(EstadoEmergencia.FINALIZADA.puedeTransicionarA(EstadoEmergencia.CONTROLADA)).isFalse();
    }

    @Test
    void noSePuedeRegresarDeControladaAActiva() {
        assertThat(EstadoEmergencia.CONTROLADA.puedeTransicionarA(EstadoEmergencia.ACTIVA)).isFalse();
    }
}
