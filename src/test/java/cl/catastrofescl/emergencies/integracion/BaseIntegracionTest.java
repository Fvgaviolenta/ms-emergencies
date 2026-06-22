package cl.catastrofescl.emergencies.integracion;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Levanta contenedores reales de PostgreSQL+PostGIS y RabbitMQ, compartidos por todos
 * los tests de integracion que extiendan esta clase.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = BaseIntegracionTest.Inicializador.class)
public abstract class BaseIntegracionTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.4")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("catastrofescl_emergencies_test")
            .withUsername("catastrofescl")
            .withPassword("catastrofescl");

    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management"));

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }

    public static class Inicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                    "spring.datasource.username=" + POSTGRES.getUsername(),
                    "spring.datasource.password=" + POSTGRES.getPassword(),
                    "spring.rabbitmq.host=" + RABBITMQ.getHost(),
                    "spring.rabbitmq.port=" + RABBITMQ.getAmqpPort(),
                    "spring.rabbitmq.username=" + RABBITMQ.getAdminUsername(),
                    "spring.rabbitmq.password=" + RABBITMQ.getAdminPassword(),
                    // Redis no es necesario para estos tests: se desactiva la autoconfiguracion
                    "spring.autoconfigure.exclude=" +
                            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
            ).applyTo(context.getEnvironment());
        }
    }
}
