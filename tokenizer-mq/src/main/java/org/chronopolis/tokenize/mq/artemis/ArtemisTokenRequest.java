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
 * Implementation for a TokenRequestBatch using ActiveMQ Artemis to receive requests
 *
 * @author shake
 */
public class ArtemisTokenRequest implements Runnable, Closeable {

    private final Logger log = LoggerFactory.getLogger(ArtemisTokenRequest.class);

    private int attempts = 0;
    private final long timeout;
    private final TimeUnit unit;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final ObjectMapper mapper;
    private final ImsServiceWrapper ims;
    private final ServerLocator serverLocator;
    private final ArtemisSupervisor supervisor;

    /**
     * Constructor for a consumer which creates ACE Token Requests
     *
     * @param timeout       the timeout to wait if we did not receive any messages from the broker
     * @param ims           a wrapper for creating connections to the ACE IMS
     * @param supervisor    the TokenWorkSupervisor managing the flow of messages
     * @param serverLocator the MQ Server Locator
     * @param mapper        the ObjectMapper for deserializing messages
     */
    public ArtemisTokenRequest(long timeout,
                               ImsServiceWrapper ims,
                               ArtemisSupervisor supervisor,
                               ServerLocator serverLocator,
                               ObjectMapper mapper) {
        this(timeout, TimeUnit.MINUTES, ims, supervisor, serverLocator, mapper);
    }

    /**
     * Constructor for an ArtemisTokenRequest which also takes a TimeUnit. Useful for testing.
     *
     *
     * @param timeout       the timeout to wait if we did not receive any messages from the broker
     * @param unit          the unit of time to wait when executing a timeout
     * @param ims           a wrapper for creating connections to the ACE IMS
     * @param supervisor    the TokenWorkSupervisor managing the flow of messages
     * @param serverLocator the MQ Server Locator
     * @param mapper        the ObjectMapper for deserializing messages
     */
    ArtemisTokenRequest(long timeout,
                        TimeUnit unit,
                        ImsServiceWrapper ims,
                        ArtemisSupervisor supervisor,
                        ServerLocator serverLocator,
                        ObjectMapper mapper) {
        this.timeout = timeout;
        this.unit = unit;
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

        TimeUnit unit = TimeUnit.MILLISECONDS;
        final Long timeout = (long) ims.configuration().getWaitTime();
        final int batchSize = ims.configuration().getQueueLength();
        final long deadline = System.nanoTime() + unit.toNanos(timeout);
        long localTimeout = deadline;

        try {
            // todo: what if we change this to work as follows:
            // 1: Poll the broker for a message
            // 2: Send the message to a tokenrequestbatch
            // 3: Have the tokenrequestbatch block on (size == batch_size || timeout)
            // I'm not the biggest fan of constantly polling for messages, but since we can't ask
            // the broker for a batch it seems to be the best we can do
            while (receivedMessages.size() < batchSize && localTimeout > 0) {
                long l = TimeUnit.NANOSECONDS.toMillis(localTimeout);

                // mostly a sanity check but I've seen some... very large values so I'm curious
                // if there's a bug here or below
                if (l > timeout) {
                    l = timeout;
                }

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

            if (!receivedMessages.isEmpty() && running.get()) {
                log.info("[Tokenizer] Processing {} messages", receivedMessages.size());
                attempts = 0;
                processMessages(receivedMessages);
            } else {
                attempts++;
            }
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
                if (!running.get()) {
                    break;
                }

                ManifestEntry entry = entries.remove(token.getName());
                supervisor.associate(entry, token);
            }
        } catch (Exception e) {
            log.info("Error communicating with the ims", e);
        }
    }

    private TokenRequest createRequest(ManifestEntry entry) {
        TokenRequest request = new TokenRequest();
        request.setHashValue(entry.getDigest());
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
            while (running.get() && attempts <= 5) {
                consume(consumer);

                if (attempts > 0 && attempts <= 5) {
                    log.debug("Unable to poll from message broker; sleeping before retry");
                    unit.sleep(timeout);
                }
            }
        } catch (Exception e) {
            close();
            log.warn("Closing consumer", e);
        }

        log.info("Closing ArtemisTokenRequest");
    }

    @Override
    public void close() {
        log.info("Stopping ArtemisTokenRequest");
        running.set(false);
    }

}
