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

    private final TokenService tokens;
    private final ObjectMapper mapper;
    private final ServerLocator serverLocator;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ArtemisTokenRegistrar(TokenService tokens,
                                 ServerLocator serverLocator,
                                 ObjectMapper mapper) {
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
                String id = message.getStringProperty("id");
                if (id != null && !id.isEmpty()) {
                    message.individualAcknowledge();
                    boolean rollback = registerToken(message);

                    if (rollback) {
                        session.rollback();
                    }
                }
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

        // If this fails, it will look as though the program is hanging here when in fact it's
        // probably a NoClassDef exception because the ace-ims-api package relies on log4j
        String proof = IMSUtil.formatProof(tokenResponse);
        Optional<String> filename = getFilename(tokenResponse);
        Optional<AceTokenModel> optModel = filename.map(name -> new AceTokenModel()
                .setProof(proof)
                .setFilename(name)
                .setBagId(bag.getId())
                .setImsHost("ims.umiacs.umd.edu")
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

    @Override
    public void run() {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createTransactedSession();
             ClientConsumer consumer = session.createConsumer(ArtemisSupervisor.REGISTER_TOPIC)) {
            session.start();
            while (running.get()) {
                consume(consumer, session);
            }
        } catch (Exception e) {
            log.warn("Closing consumer", e);
            running.set(false);
        }
    }

    @Override
    public void close() {
        log.info("Stopping ArtemisTokenRegistrar");
        running.set(false);
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
        log.debug("[{}] Attempting to register token at {}",
                tokenResponse.getName(), call.request().url().toString());
        boolean rollback = false;

        try {
            Response<AceTokenModel> response = call.execute();
            int responseCode = response.code();
            if (response.isSuccessful() || responseCode == 409) {
                log.info("AceToken registered with the Ingest Server");
            } else {
                log.warn("Unable to register AceToken! Response Code is {}", responseCode);
            }

            if (responseCode >= 500) {
                rollback = true;
                log.warn("Server error ({}), session should be rolled back", responseCode);
            }
        } catch (IOException e) {
            rollback = true;
            log.warn("Error communicating with server, session should be rolled back", e);
        }

        return rollback;
    }
}
