package org.chronopolis.tokenize.mq.artemis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Fixity;
import org.chronopolis.rest.models.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.chronopolis.rest.models.serializers.FixityAlgorithmDeserializer;
import org.chronopolis.rest.models.serializers.FixityAlgorithmSerializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.ManifestEntryDeserializer;
import org.chronopolis.tokenize.batch.ImsServiceWrapper;
import org.chronopolis.tokenize.mq.RegisterMessage;
import org.junit.After;
import org.junit.Before;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.emptySet;
import static org.mockito.Mockito.mock;

/**
 * Superclass with some common functionality for testing
 *
 * @author shake
 */
public class MqTest {

    static final long ID = 1L;
    private static final String NAME = "test-name";
    private static final String DIGEST = "digest";
    private static final String FILENAME = "filename";
    private static final String ALGORITHM = "SHA-256";
    private static final String DEPOSITOR = "test-depositor";
    private static final String ID_PROPERTY = "id";
    private static final String BAG_ID_PROPERTY = "bagId";
    private static final String RESPONSE_FORMAT = "(%s,%s)::%s";
    private static final ZonedDateTime NOW = ZonedDateTime.now();

    public static final String EMPTY_FIXITY =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final SimpleString REQUEST_QUEUE = new SimpleString("request");
    private static final SimpleString REGISTER_QUEUE = new SimpleString("register");


    TokenService tokens;
    ServerLocator serverLocator;
    ImsServiceWrapper imsWrapper;
    ThreadPoolExecutor executor;
    private final EmbeddedActiveMQResource activeMQ = new EmbeddedActiveMQResource();

    final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void startup() throws Exception {
        activeMQ.start();
        activeMQ.createQueue(REQUEST_QUEUE.toString());
        activeMQ.createQueue(REGISTER_QUEUE.toString());


        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addSerializer(FixityAlgorithm.class, new FixityAlgorithmSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        module.addDeserializer(ManifestEntry.class, new ManifestEntryDeserializer());
        module.addDeserializer(FixityAlgorithm.class, new FixityAlgorithmDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new KotlinModule());

        serverLocator = ActiveMQClient.createServerLocator(activeMQ.getVmURL());

        tokens = mock(TokenService.class);
        imsWrapper = mock(ImsServiceWrapper.class);
        executor = mock(ThreadPoolExecutor.class);
    }

    @After
    public void tearDown() {
        activeMQ.stop();
    }

    Bag createBag() {
        Fixity fixity = new Fixity(EMPTY_FIXITY, FixityAlgorithm.SHA_256, NOW);
        StagingStorage storage = new StagingStorage(true, ID, ID, ID, FILENAME, of(fixity));
        return new Bag(ID, ID, ID, storage, storage, NOW, NOW,
                NAME, DEPOSITOR, DEPOSITOR, BagStatus.DEPOSITED, emptySet());
    }

    ManifestEntry createEntry() {
        return new ManifestEntry(createBag(), FILENAME, DIGEST);
    }

    TokenResponse createTokenResponse() throws DatatypeConfigurationException {
        TokenResponse response = new TokenResponse();
        response.setName(String.format(RESPONSE_FORMAT, DEPOSITOR, NAME, FILENAME));
        response.setTokenClassName(ALGORITHM);
        response.setDigestService(ALGORITHM);
        response.setDigestProvider(ALGORITHM);
        response.setStatusCode(201);
        response.setRoundId(ID);
        XMLGregorianCalendar calendar = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(GregorianCalendar.from(NOW));
        response.setTimestamp(calendar);
        return response;
    }

    void sendRequestMessage() throws Exception {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            ClientMessage message = session.createMessage(false);
            ClientProducer producer = session.createProducer(REQUEST_QUEUE);
            ManifestEntry entry = createEntry();
            message.putLongProperty(BAG_ID_PROPERTY, entry.getBag().getId());
            message.putStringProperty(ID_PROPERTY, entry.tokenName());
            message.writeBodyBufferString(mapper.writeValueAsString(entry));
            producer.send(message);
        }
    }


    void sendRegisterMessage() throws Exception {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            ManifestEntry entry = createEntry();
            RegisterMessage rMsg = new RegisterMessage();
            rMsg.setBag(createBag());
            rMsg.setToken(createTokenResponse());
            ClientMessage message = session.createMessage(false);
            message.putLongProperty(new SimpleString(BAG_ID_PROPERTY), ID);
            message.putStringProperty(new SimpleString(ID_PROPERTY), entry.tokenName());
            message.writeBodyBufferString(mapper.writeValueAsString(rMsg));

            ClientProducer producer = session.createProducer(REGISTER_QUEUE);
            producer.send(message);
        }
    }
}
