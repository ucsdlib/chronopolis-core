package org.chronopolis.tokenize.mq.artemis;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.mq.RegisterMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple consumer which registers tokens
 *
 * @author shake
 */
public class ArtemisTokenRegistrar implements Runnable, Closeable {
    private final Logger log = LoggerFactory.getLogger(ArtemisTokenRegistrar.class);

    private int attempts = 0;
    private final Long timeout;
    private final TimeUnit unit;
    private final TokenService tokens;
    private final ObjectMapper mapper;
    private final ServerLocator serverLocator;
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Constructor for a TokenRegistrar
     *
     * @param timeout       the timeout (in minutes) to wait if we did not receive any message
     * @param tokens        the api for connecting to the Ingest Server Token API
     * @param serverLocator the MQ Server Locator
     * @param mapper        an ObjectMapper for deserializing messages
     */
    public ArtemisTokenRegistrar(long timeout,
                                 TokenService tokens,
                                 ServerLocator serverLocator,
                                 ObjectMapper mapper) {
        this(timeout, TimeUnit.MINUTES, tokens, serverLocator, mapper);
    }

    /**
     * Constructor for a TokenRegistrar which also takes a TimeUnit. Useful for testing.
     *
     *
     *
     * @param timeout       the length of time to wait if we did not receive any message
     * @param unit          the unit of time to wait when executing a timeout
     * @param tokens        the api for connecting to the Ingest Server Token API
     * @param serverLocator the MQ Server Locator
     * @param mapper        an ObjectMapper for deserializing messages
     */
    ArtemisTokenRegistrar(long timeout,
                          TimeUnit unit,
                          TokenService tokens,
                          ServerLocator serverLocator,
                          ObjectMapper mapper) {
        this.timeout = timeout;
        this.unit = unit;
        this.tokens = tokens;
        this.serverLocator = serverLocator;
        this.mapper = mapper;
    }

    private void consume(ClientConsumer consumer, ClientSession session) {
        // we might want to listen on a more general address, such as processing.#.register
        // this would let us track bags which are still in progress
        // e.g. processing.depositor0-bag0.register
        try {
            ClientMessage message = consumer.receive(1000);
            if (message != null) {
                // reset our counter
                attempts = 0;

                String id = message.getStringProperty("id");
                if (id != null && !id.isEmpty()) {
                    message.individualAcknowledge();
                    boolean rollback = registerToken(message);

                    if (rollback) {
                        log.debug("Rolling back session");
                        session.rollback();
                    }
                }
                session.commit();
            } else {
                attempts++;
            }
        } catch (ActiveMQException e) {
            log.error("Exception communicating to message broker", e);
        }
    }

    private boolean registerToken(ClientMessage clientMessage) {
        Bag bag;
        TokenResponse tokenResponse;
        try {
            String text = clientMessage.getReadOnlyBodyBuffer().readString();
            RegisterMessage message = mapper.readValue(text, RegisterMessage.class);
            bag = message.getBag();
            tokenResponse = message.getToken();
        } catch (IOException e) {
            log.error("Error deserializing received message", e);
            return false;
        }

        // Eventually we might want to try to pull this from the TokenResponse or from the config
        String imsHost = "ims.umiacs.umd.edu";

        // If this fails, it will look as though the program is hanging here when in fact it's
        // probably a NoClassDef exception because the ace-ims-api package relies on log4j
        String proof = IMSUtil.formatProof(tokenResponse);
        Optional<String> filename = getFilename(tokenResponse);
        Optional<AceTokenModel> optModel = filename.map(name -> new AceTokenModel()
                .setProof(proof)
                .setFilename(name)
                .setBagId(bag.getId())
                .setImsHost(imsHost)
                .setRound(tokenResponse.getRoundId())
                .setAlgorithm(tokenResponse.getDigestService())
                .setImsService(tokenResponse.getTokenClassName())
                .setCreateDate(tokenResponse.getTimestamp()
                        .toGregorianCalendar()
                        .toZonedDateTime()));

        return optModel.map(model -> tokens.createToken(bag.getId(), model))
                .map(call -> accept(call, tokenResponse))
                .orElse(false);
    }

    /**
     * Match a TokenResponse's name against a known regex in order to extract the filename
     *
     * @param tokenResponse the token response to match against
     * @return the filename, if present
     */
    private Optional<String> getFilename(TokenResponse tokenResponse) {
        Pattern pattern = Pattern.compile("\\(.*?,.*?\\)::(.*)");

        Matcher matcher = pattern.matcher(tokenResponse.getName());
        if (matcher.matches()) {
            return Optional.of(matcher.group(matcher.groupCount()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Execute the http call. If it is successful ack the message.
     * <p>
     * - rollback if an exception is thrown (probably communicating with the server)
     * - rollback if we receive a 5xx
     * - ack if we receive a 401,403,409, etc
     * <p>
     * We should probably return an enum instead of a boolean to really encapsulate the intentions
     * of the return value
     *
     * @param call          the http call to execute
     * @param tokenResponse the ACE Token Response to upload
     * @return if the current transaction should be rolled back
     */
    private boolean accept(Call<AceTokenModel> call, TokenResponse tokenResponse) {
        boolean rollback = false;
        String tokenName = tokenResponse.getName();
        log.debug("[{}] Attempting to register token at {}",
                tokenName, call.request().url().toString());

        try {
            Response<AceTokenModel> response = call.execute();
            int responseCode = response.code();
            if (response.isSuccessful() || responseCode == 409) {
                log.info("[{}] Registered", tokenName);
            } else {
                log.warn("[{}] Unable to register! Response Code is {}", tokenName, responseCode);
            }

            if (responseCode >= 500) {
                rollback = true;
                log.warn("[{}] Server error ({})", tokenName, responseCode);
            }
        } catch (IOException e) {
            rollback = true;
            log.warn("[{}] Error communicating with server", tokenName, e);
        }

        return rollback;
    }

    @Override
    public void run() {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createTransactedSession();
             ClientConsumer consumer = session.createConsumer(ArtemisSupervisor.REGISTER_TOPIC)) {
            session.start();
            while (running.get() && attempts <= 5) {
                consume(consumer, session);

                // if we failed to consume anything, wait for a given amount of time
                if (attempts > 0 && attempts <= 5) {
                    log.debug("Unable to poll from broker; sleeping before retry");
                    unit.sleep(timeout);
                }
            }
        } catch (Exception e) {
            close();
            log.warn("Closing consumer", e);
        }

        log.info("Closing ArtemisTokenRegistrar");
    }

    @Override
    public void close() {
        log.info("Stopping ArtemisTokenRegistrar");
        running.set(false);
    }

}
