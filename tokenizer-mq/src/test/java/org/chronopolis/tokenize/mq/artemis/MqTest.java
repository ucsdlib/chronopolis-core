package org.chronopolis.tokenize.mq.artemis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import org.chronopolis.rest.models.BagStatus;
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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.concurrent.ThreadPoolExecutor;

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

    private static final SimpleString REQUEST_QUEUE = new SimpleString("request");
    private static final SimpleString REGISTER_QUEUE = new SimpleString("register");


    TokenService tokens;
    ServerLocator serverLocator;
    ImsServiceWrapper imsWrapper;
    ThreadPoolExecutor executor;
    private EmbeddedActiveMQResource activeMQ = new EmbeddedActiveMQResource();

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void startup() throws Exception {
        activeMQ.start();
        activeMQ.createQueue(REQUEST_QUEUE.toString());
        activeMQ.createQueue(REGISTER_QUEUE.toString());


        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        module.addDeserializer(ManifestEntry.class, new ManifestEntryDeserializer());
        mapper.registerModule(module);

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
        return new Bag()
                .setId(ID)
                .setName(NAME)
                .setDepositor(DEPOSITOR)
                .setCreatedAt(NOW)
                .setUpdatedAt(NOW)
                .setCreator(DEPOSITOR)
                .setSize(ID)
                .setStatus(BagStatus.DEPOSITED)
                .setTotalFiles(ID)
                .setReplicatingNodes(Collections.emptySet());
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
