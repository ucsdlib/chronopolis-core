package org.chronopolis.amqp;

import java.io.IOException;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.exception.InvalidMessageException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;


/**
 * Listener which receives notifications from AMQP when chronopolis messages are
 * received by various services
 *
 * @author toaster
 */
public abstract class ChronMessageListener implements MessageListener {
    private Logger log = LoggerFactory.getLogger(ChronMessageListener.class);

    @Override
    public void onMessage(Message msg) {
        MessageProperties props = msg.getMessageProperties();
        byte[] body = msg.getBody();
        ChronMessage message = null;

        if (null == props) {
            throw new IllegalArgumentException("Message properties are null!");
        }

        if (null == props.getHeaders() || props.getHeaders().isEmpty()) {
            throw new IllegalArgumentException("Message headers are empty!");
        }

        ChronBody chronBody = getChronBody(body);
        message = ChronMessage.getMessage(chronBody.getType());
        message.setHeader(props.getHeaders());
        message.setBody(chronBody);

        // Sanity Check
        if (null != message) {
            log.debug("Received {}", message);
            ChronProcessor processor = getProcessor(message.getType());
            try {
                log.info("Processing {}", message.getType());
                processor.process(message);
            } catch (Exception e) {
                log.error("Unexpected processing error {} ", e);
            }
        }
    }

    private ChronBody getChronBody(byte[] body) {
        if (body == null) {
            throw new IllegalArgumentException("Message body is null.");
        }

        String json = new String(body);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        try {
            return mapper.readValue(json, ChronBody.class);
        } catch (IOException e) {
            throw new InvalidMessageException(json, e);
        }
    }

    public abstract ChronProcessor getProcessor(MessageType type);
}
