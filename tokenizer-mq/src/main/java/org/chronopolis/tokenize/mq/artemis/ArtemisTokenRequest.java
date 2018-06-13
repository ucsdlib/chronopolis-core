package org.chronopolis.tokenize.mq.artemis;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.batch.ImsServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Proto implementation for a TokenRequestBatch using ActiveMQ Artemis to receive requests
 *
 * @author shake
 */
public class ArtemisTokenRequest implements Runnable, Closeable {

    private final Logger log = LoggerFactory.getLogger(ArtemisTokenRequest.class);

    private TimeUnit unit = TimeUnit.MILLISECONDS;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final ObjectMapper mapper;
    private final ImsServiceWrapper ims;
    private final ServerLocator serverLocator;
    private final ArtemisSupervisor supervisor;

    public ArtemisTokenRequest(ImsServiceWrapper ims,
                               ArtemisSupervisor supervisor,
                               ServerLocator serverLocator,
                               ObjectMapper mapper) {
        this.ims = ims;
        this.mapper = mapper;
        this.supervisor = supervisor;
        this.serverLocator = serverLocator;
    }

    /**
     * Capture n messages where n < batchSize and use them to create a TokenRequest to the ACE IMS
     *
     * @param consumer the MQ consumer to retrieve messages with
     */
    private void consume(ClientConsumer consumer) {
        Map<String, ClientMessage> receivedMessages = new HashMap<>();
        final Long timeout = (long) ims.configuration().getWaitTime();
        final int batchSize = ims.configuration().getQueueLength();
        final long deadline = System.nanoTime() + unit.toNanos(timeout);
        long localTimeout = deadline;

        try {
            // I'm not the biggest fan of constantly polling for messages, but since we can't ask
            // the broker for a batch it seems to be the best we can do
            while (receivedMessages.size() < batchSize && localTimeout > 0) {
                long l = TimeUnit.NANOSECONDS.toMillis(localTimeout);
                ClientMessage received;
                received = consumer.receive(l);
                if (received != null) {
                    String id = received.getStringProperty("id");
                    if (id != null && !id.isEmpty()) {
                        received.individualAcknowledge();
                        receivedMessages.put(id, received);
                    }
                }

                localTimeout = deadline - System.nanoTime();
            }

            if (!receivedMessages.isEmpty()) {
                log.info("[Tokenizer] Processing {} messages", receivedMessages.size());
                processMessages(receivedMessages);
            }

            // session.commit();
        } catch (ActiveMQException e) {
            log.warn("Error with ActiveMQ", e);
        }
    }

    private void processMessages(Map<String, ClientMessage> receivedMessages) {
        final List<TokenRequest> requests = new ArrayList<>();
        final Map<String, ManifestEntry> entries = new HashMap<>();

        // Deserialize and create a TokenRequest for each message we received
        for (ClientMessage message : receivedMessages.values()) {
            try {
                String body = message.getReadOnlyBodyBuffer().readString();
                ManifestEntry entry = mapper.readValue(body, ManifestEntry.class);
                requests.add(createRequest(entry));
                entries.put(entry.tokenName(), entry);
            } catch (JsonParseException e) {
                log.error("Invalid json", e);
            } catch (IOException e) {
                log.error("Error parsing message", e);
            }
        }

        // will probably want to revisit how we handle exceptions here
        // notably because communication failure with the ims should just involve rolling back
        // messages instead of just acking them
        // boolean interrupted = false;
        String tokenClass = ims.configuration().getTokenClass();
        try {
            List<TokenResponse> tokens = ims.requestTokensImmediate(tokenClass, requests);
            for (TokenResponse token : tokens) {
                ManifestEntry entry = entries.remove(token.getName());
                supervisor.associate(entry, token);
            }
        } catch (Exception e) {
            log.info("Error communicating with the ims", e);
        }
    }

    private TokenRequest createRequest(ManifestEntry entry) {
        TokenRequest request = new TokenRequest();
        request.setHashValue(entry.getRegisteredDigest());
        request.setName(entry.tokenName());
        return request;
    }

    // Basic methods for running continuously and triggering a stop

    @Override
    public void run() {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession();
             ClientConsumer consumer = session.createConsumer(ArtemisSupervisor.REQUEST_TOPIC)) {
            session.start();
            while (running.get()) {
                consume(consumer);
            }
        } catch (Exception e) {
            log.warn("Closing consumer", e);
        }
    }

    @Override
    public void close() {
        log.info("Stopping ArtemisTokenRequest");
        running.set(false);
    }

}
