/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import java.util.Map;
import org.chronopolis.messaging.base.ChronMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author shake
 */
public class TopicProducer implements ChronProducer {

    private final Logger log = LoggerFactory.getLogger(TopicProducer.class);

    private final RabbitTemplate template;
    private String defaultRoutingKey;

    public TopicProducer(RabbitTemplate template) {
        this.template = template;
    }

    @Override
    public void send(final ChronMessage message, String routingKey) {
        boolean done = false;
        int numTries = 0;
        log.debug("Preparing message {}",  message.toString());
        MessageProperties props = new MessageProperties();
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setContentType("application/json");

        if (null == routingKey) {
            routingKey = defaultRoutingKey;
        }

        Map<String, Object> headers = message.getHeader();
        if (headers != null && !headers.isEmpty()) {
            for (String key : headers.keySet()) {
                props.setHeader(key, headers.get(key));
            }
        } else {
            log.error("Message headers not valid!");
            throw new RuntimeException("Invalid headers");
        }

        while (!done && numTries < 3) {
            try {
                Message msg = new Message(message.toJson().getBytes(), props);
                log.info("Sending {} to {} ", message.getType(), routingKey);

                template.send(routingKey, msg);
                done = true;
            } catch (AmqpException e) {
                log.error("Error publishing '{}', retrying", message, e);

                numTries++;
            }
        }


    }



}
