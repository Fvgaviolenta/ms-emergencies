package cl.catastrofescl.emergencies.mensajeria;

import cl.catastrofescl.emergencies.event.EmergenciaCreadaEvento;
import cl.catastrofescl.emergencies.service.ServicioProcesamientoEmergenciaCreada;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor local de {@code emergency.created}: genera un anuncio vigente (visible en
 * {@code GET /announcements}) y registra el procesamiento idempotente del mensaje.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorEventoEmergenciaCreada {

    private final ServicioProcesamientoEmergenciaCreada servicioProcesamiento;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${catastrofescl.rabbitmq.cola-emergencia-creada}",
                            durable = "true"),
                    exchange = @Exchange(value = "${catastrofescl.rabbitmq.exchange-topic}",
                            type = ExchangeTypes.TOPIC),
                    key = "emergency.created"
            ),
            messageConverter = "messageConverter"
    )
    public void alRecibirEmergenciaCreada(EmergenciaCreadaEvento evento) {
        log.debug("Mensaje emergency.created recibido emergenciaId={}", evento.getEmergenciaId());
        servicioProcesamiento.procesar(evento);
    }
}
