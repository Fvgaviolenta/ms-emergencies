package cl.catastrofescl.emergencies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio ms-emergencies.
 */
@SpringBootApplication
public class EmergenciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergenciasApplication.class, args);
    }
}
