package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.event.EventoDominio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de dominio en el topic exchange configurado.
 * Agrega un correlationId al mensaje para trazabilidad distribuida.
 */
@Slf4j
@Component
public class PublicadorEventos {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeTopic;

    public PublicadorEventos(RabbitTemplate rabbitTemplate,
                             @Value("${catastrofescl.rabbitmq.exchange-topic}") String exchangeTopic) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeTopic = exchangeTopic;
    }

    public void publicar(EventoDominio evento) {
        log.debug("Publicando evento {} (eventId={}) con routing key '{}'",
                evento.getClass().getSimpleName(), evento.getEventoId(), evento.getRoutingKey());

        rabbitTemplate.convertAndSend(exchangeTopic, evento.getRoutingKey(), evento, mensaje -> {
            mensaje.getMessageProperties().setMessageId(evento.getEventoId().toString());
            mensaje.getMessageProperties().setCorrelationId(evento.getCorrelacionId());
            mensaje.getMessageProperties().setContentType("application/json");
            return mensaje;
        });
    }
}
