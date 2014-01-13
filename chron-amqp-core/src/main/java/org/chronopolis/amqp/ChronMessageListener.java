package org.chronopolis.amqp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Listener which receives notifications from AMQP when chronopolis messages are
 * received by various services
 * 
 * @author toaster
 */
public abstract class ChronMessageListener implements MessageListener {
	// private ChronProcessor processor;
	// private Map<MessageType, ChronProcessor> processors;
    private Logger log = LoggerFactory.getLogger(ChronMessageListener.class);
    
	/*
    public ChronMessageListener(ChronProcessor... processors) {
		this.processors = new HashMap<>();
		for ( ChronProcessor p : processors ) {
			this.processors.put(p.getMessageType(), p);
		}
    }
	*/
    
	@Override
    public void onMessage(Message msg) {
        MessageProperties props = msg.getMessageProperties();
        byte[] body = msg.getBody();
        ChronMessage message = null;

        if ( null == body ) {
            throw new IllegalArgumentException("Message body is null!");
        }
        if ( null == props ) {
            throw new IllegalArgumentException("Message proerties are null!");
        }
        if ( null == props.getHeaders() || props.getHeaders().isEmpty()) {
            throw new IllegalArgumentException("Messgae headers are empty!");
        }

        // Ok, so this still looks kind of crufty, but I don't think there's any
        // way to get around dealing with the byte streams to create our object
        // since we don't use an ObjectMapper (which honestly isn't much better)
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            if ( !(o instanceof ChronBody)) {
                log.info("Recieved object not of type ChronBody");
                throw new IllegalArgumentException("Message body is not a chron body!");
            }

            ChronBody cBody = (ChronBody) o;
            log.debug("Recieved Body of ChronMessage of type {} ", cBody.getType().toString());

            message = ChronMessage.getMessage(cBody.getType());
            System.out.println("Message Type: " + cBody.getType().toString());
            message.setBody(cBody);
            message.setHeader(props.getHeaders());
            
        } catch (IOException ex) {
            log.error("Exception reading message: " + ex.toString());
        } catch (ClassNotFoundException ex) {
            log.error("Class not found when reading in message: " + ex.toString());
        }

        // Sanity Check
        if ( null != message ) {
			ChronProcessor processor = getProcessor(message.getType());
            try { 
                processor.process(message);
            } catch (Exception e){
                log.error("Unexpected processing error {} ", e);
            }
        }
    }

	public abstract ChronProcessor getProcessor(MessageType type);
}
