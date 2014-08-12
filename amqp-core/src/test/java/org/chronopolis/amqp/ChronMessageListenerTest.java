package org.chronopolis.amqp;

import junit.framework.TestCase;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.chronopolis.messaging.MessageConstant.CORRELATION_ID;
import static org.chronopolis.messaging.MessageConstant.DATE;
import static org.chronopolis.messaging.MessageConstant.ORIGIN;
import static org.chronopolis.messaging.MessageConstant.RETURN_KEY;

/**
 * Created by shake on 2/4/14.
 */
public class ChronMessageListenerTest extends TestCase {
    // Uh forgot this was abstract but ok
    private TestProcessor processor = new TestProcessor();
    private ChronMessageListener listener = new ChronMessageListener() {
        @Override
        public ChronProcessor getProcessor(MessageType type) {
            if ( type.equals(MessageType.COLLECTION_INIT)) {
                return processor;
            }
            return null;
        }
    };

    private MessageFactory messageFactory;

    public void setUp() throws Exception {
        super.setUp();
        ChronopolisSettings settings = new ChronopolisSettings();
        messageFactory = new MessageFactory(settings);
    }

    public void testOnMessage() throws Exception {
        CollectionInitMessage chronMessage = messageFactory.defaultCollectionInitMessage();
        processor.setExpected(chronMessage);
        String json = chronMessage.toJson();

        MessageProperties msgProps = new MessageProperties();
        msgProps.setHeader(CORRELATION_ID.toString(), chronMessage.getCorrelationId());
        msgProps.setHeader(DATE.toString(), chronMessage.getDate());
        msgProps.setHeader(ORIGIN.toString(), chronMessage.getOrigin());
        msgProps.setHeader(RETURN_KEY.toString(), chronMessage.getReturnKey());
        Message message = new Message(json.getBytes(), msgProps);

        listener.onMessage(message);
    }

    public void testGetProcessor() throws Exception {
        ChronProcessor p1 = listener.getProcessor(MessageType.COLLECTION_INIT);
        ChronProcessor p2 = listener.getProcessor(MessageType.COLLECTION_INIT_COMPLETE);

        assertEquals(processor, p1);
        assertNull(p2);
    }

}
