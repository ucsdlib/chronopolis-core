package org.chronopolis.tokenize.mq.artemis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.batch.ImsServiceWrapper;
import org.chronopolis.tokenize.mq.RegisterMessage;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * TokenWorkSupervisor which connects to an Artemis Broker to control flow for operations
 *
 * @author shake
 */
public class ArtemisSupervisor implements TokenWorkSupervisor, Closeable {
    private final Logger log = LoggerFactory.getLogger(ArtemisSupervisor.class);

    private static final boolean BROWSE_ONLY = true;
    private static final String ID_PROPERTY = "id";
    private static final String BAG_ID_PROPERTY = "bagId";
    static final String REQUEST_TOPIC = "request";
    static final String REGISTER_TOPIC = "register";

    private final ObjectMapper mapper;
    private final TokenService tokens;
    private final ImsServiceWrapper ims;
    private final ServerLocator locator;
    private final ClientSessionFactory sessionFactory;

    private final ThreadPoolExecutor request;
    private final ThreadPoolExecutor register;

    public ArtemisSupervisor(ServerLocator serverLocator,
                             ObjectMapper mapper,
                             TokenService tokens,
                             ImsServiceWrapper ims) throws Exception {
        this(serverLocator, mapper, tokens, ims,
                new ThreadPoolExecutor(1, 1, 0, MILLISECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(4, 4, 0, MILLISECONDS, new LinkedBlockingQueue<>()));
    }

    ArtemisSupervisor(ServerLocator serverLocator,
                      ObjectMapper mapper,
                      TokenService tokens,
                      ImsServiceWrapper ims,
                      ThreadPoolExecutor requestExecutor,
                      ThreadPoolExecutor registerExecutor) throws Exception {
        this.ims = ims;
        this.tokens = tokens;
        this.mapper = mapper;
        this.locator = serverLocator;
        this.request = requestExecutor;
        this.register = registerExecutor;
        this.sessionFactory = serverLocator.createSessionFactory();

        // init consumers here?
    }

    /**
     * Close ALL resources used by the Supervisor. This must be called when removing a Supervisor
     * in order to clean up any lingering MQ connections.
     */
    @Override
    public void close() {
        if (sessionFactory != null) {
            sessionFactory.cleanup();
            sessionFactory.close();
        }

        request.shutdownNow();
        register.shutdownNow();
    }

    @Override
    public boolean start(ManifestEntry entry) {
        log.info("[{} - {}] Starting", entry.tokenName(), Thread.currentThread().getName());
        // todo: it would be nice to create a session and producer for each thread so that we don't
        //       init it each time
        try (ClientSession session = sessionFactory.createSession();
             ClientProducer producer = session.createProducer(REQUEST_TOPIC)) {

            ClientMessage message = session.createMessage(false);
            updateMessage(entry, message);
            String json = mapper.writeValueAsString(entry);
            message.writeBodyBufferString(json);
            producer.send(message);
            session.commit();
        } catch (ActiveMQException | JsonProcessingException e) {
            log.error("Error sending token request message", e);
            return false;
        }

        checkConsumers();
        return true;
    }

    @Override
    public boolean associate(ManifestEntry entry, TokenResponse response) {
        final String id = response.getName();
        log.trace("Sending TokenRegister for {}", id);
        RegisterMessage message = new RegisterMessage(entry.getBag(), response);

        try (ClientSession session = sessionFactory.createSession();
             ClientProducer producer = session.createProducer(REGISTER_TOPIC)) {

            ClientMessage clMessage = session.createMessage(true);
            updateMessage(entry, clMessage);

            clMessage.writeBodyBufferString(mapper.writeValueAsString(message));
            producer.send(clMessage);
            session.commit();
        } catch (ActiveMQException | JsonProcessingException e) {
            log.error("Unable to send token register message", e);
            return false;
        }

        checkConsumers();
        return true;
    }

    private void updateMessage(ManifestEntry entry, ClientMessage message) {
        // It would be nice to have duplicate detection here but for now we'll leave it out as
        // the ids are still associated until a limit (default 2k) is reached
        message.putStringProperty(ID_PROPERTY, entry.tokenName());
        message.putLongProperty(BAG_ID_PROPERTY, entry.getBag().getId());
    }

    /**
     * Check if consumers are running for the two queues which we listen on (REQUEST_TOPIC and
     * REGISTER_TOPIC).
     * <p>
     * For the request topic, we only want a single consumer as it is generally pretty quick.
     * For the register topic, we want up to 4 consumers as there is some overhead when
     * communicating with the http api so this helps to alleviate some congestion.
     */
    private void checkConsumers() {
        int timeout = 1;
        int maxRegisterThreads = 4;
        synchronized (request) {
            if (request.getActiveCount() == 0) {
                log.debug("Starting TokenRequest consumer");
                request.submit(new ArtemisTokenRequest(timeout, ims, this, locator, mapper));

            }
        }

        synchronized (register) {
            int diff = maxRegisterThreads - register.getActiveCount();
            while (diff > 0) {
                log.debug("Starting TokenRegistrar consumer");
                register.submit(new ArtemisTokenRegistrar(timeout, tokens, locator, mapper));
                diff--;
            }
        }
    }

    @Override
    public boolean isProcessing() {
        boolean hasMessage = false;
        boolean processing = false;

        try (ClientSession session = sessionFactory.createSession();
             ClientConsumer requestQueue = session.createConsumer(REQUEST_TOPIC, BROWSE_ONLY);
             ClientConsumer registerQueue = session.createConsumer(REGISTER_TOPIC, BROWSE_ONLY)) {
            ClientMessage reqMsg = requestQueue.receiveImmediate();
            ClientMessage regMsg = registerQueue.receiveImmediate();

            hasMessage = reqMsg != null || regMsg != null;

            processing = hasMessage
                    || register.getActiveCount() > 0
                    || request.getActiveCount() > 0;
            log.debug("isProcessing hasMessage? {}", hasMessage);
            log.debug("isProcessing ? {}", processing);
        } catch (ActiveMQException e) {
            log.warn("Exception while trying to browse queue!", e);
        }

        if (hasMessage) {
            checkConsumers();
        }

        return processing;
    }

    public boolean isProcessing(Bag bag) {
        String filter = BAG_ID_PROPERTY + " = " + bag.getId();
        return isProcessing(filter);
    }

    @Override
    public boolean isProcessing(ManifestEntry entry) {
        // id is sufficient as it is unique in chronopolis
        String filter = ID_PROPERTY + " = '" + entry.tokenName() + "'";
        return isProcessing(filter);
    }

    private boolean isProcessing(String filter) {
        boolean processing = false;

        try (ClientSession session = sessionFactory.createSession();
             ClientConsumer requestQueue =
                     session.createConsumer(REQUEST_TOPIC, filter, BROWSE_ONLY);
             ClientConsumer registerQueue =
                     session.createConsumer(REGISTER_TOPIC, filter, BROWSE_ONLY)) {
            processing = requestQueue.receiveImmediate() != null
                    || registerQueue.receiveImmediate() != null;
        } catch (ActiveMQException e) {
            log.warn("Exception while trying to browse queue!", e);
        }

        return processing;
    }

    // Unused overrides

    @Override
    public boolean retryTokenize(ManifestEntry entry) {
        // could do the rollback here but it doesn't make sense imo
        return false;
    }

    @Override
    public boolean retryRegister(ManifestEntry entry) {
        return false;
    }

    @Override
    public void complete(ManifestEntry entry) {
        // no real use for this here
    }

    @Override
    public Set<ManifestEntry> queuedEntries(int size, long timeout, TimeUnit timeUnit) {
        return Collections.emptySet();
    }

    @Override
    public Map<ManifestEntry, TokenResponse> tokenizedEntries(int size,
                                                              long timeout,
                                                              TimeUnit timeUnit) {
        return Collections.emptyMap();
    }

}
