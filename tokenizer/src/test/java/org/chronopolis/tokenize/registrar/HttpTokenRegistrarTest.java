package org.chronopolis.tokenize.registrar;

import com.google.common.collect.ImmutableMap;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ErrorCallWrapper;
import org.chronopolis.test.support.ExceptingCallWrapper;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.ZonedDateTime.now;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpTokenRegistrarTest {
    private final Logger log = LoggerFactory.getLogger(HttpTokenRegistrarTest.class);

    // include extraneous characters?
    private static final String path = "/data/path/to/file.txt";
    private static final String name = "test-name";
    private static final String digest = "digest";
    private static final String service = "test-service";
    private static final String provider = "test-provider";
    private static final String depositor = "test-depositor";
    private static final String tokenClass = "test-token-class";
    private static final Long id = 1L;
    private static final Long round = 1L;
    private static final Integer status = 1;

    private ManifestEntry entry;
    private AceToken model;
    private TokenResponse response;
    private HttpTokenRegistrar registrar;

    @Mock private TokenService tokens;
    @Mock private TokenWorkSupervisor supervisor;

    @Before
    public void setup() throws DatatypeConfigurationException {
        tokens = mock(TokenService.class);
        supervisor = mock(TokenWorkSupervisor.class);

        Bag bag = new Bag(id, id, id, null, null, now(), now(), name, depositor, depositor,
                BagStatus.DEPOSITED, new HashSet<>());
        entry = new ManifestEntry(bag, path, digest);

        response = new TokenResponse();
        response.setDigestProvider(provider);
        response.setDigestService(service);
        response.setName(entry.tokenName());
        response.setRoundId(round);
        response.setStatusCode(status);

        XMLGregorianCalendar calendar = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(GregorianCalendar.from(now()));
        response.setTimestamp(calendar);
        response.setTokenClassName(tokenClass);

        final String host = "test-ims-endpoint";
        final String proof = "test-proof";

        AceConfiguration configuration = new AceConfiguration()
                .setIms(new AceConfiguration.Ims().setEndpoint(host));
        registrar = new HttpTokenRegistrar(tokens, supervisor, configuration);

        model = new AceToken(id, id, round, proof, host, path, provider, service, now());
    }

    @Test
    public void get() {
        CallWrapper<AceToken> success = new CallWrapper<>(model);
        when(tokens.createToken(eq(id), any(AceTokenCreate.class))).thenReturn(success);

        registrar.register(ImmutableMap.of(entry, response));
        verify(tokens, times(1)).createToken(eq(id), any(AceTokenCreate.class));
        verify(supervisor, times(1)).complete(eq(entry));
    }

    @Test
    public void registerFailWithException() {
        ExceptingCallWrapper<AceToken> exception = new ExceptingCallWrapper<>(model);

        when(tokens.createToken(eq(id), any(AceTokenCreate.class))).thenReturn(exception);

        registrar.register(ImmutableMap.of(entry, response));
        verify(tokens, times(1)).createToken(eq(id), any(AceTokenCreate.class));
        verify(supervisor, times(1)).retryRegister(eq(entry));
    }

    @Test
    public void registerFail4xxError() {
        ErrorCallWrapper<AceToken> error = new ErrorCallWrapper<>(model, 404, "Bag not found");
        when(tokens.createToken(eq(id), any(AceTokenCreate.class))).thenReturn(error);

        registrar.register(ImmutableMap.of(entry, response));
        verify(tokens, times(1)).createToken(eq(id), any(AceTokenCreate.class));
        verify(supervisor, times(1)).complete(eq(entry));
    }

    @Test
    public void register409Success() {
        ErrorCallWrapper<AceToken> error = new ErrorCallWrapper<>(model, 409, "Token exists");
        when(tokens.createToken(eq(id), any(AceTokenCreate.class))).thenReturn(error);

        registrar.register(ImmutableMap.of(entry, response));
        verify(tokens, times(1)).createToken(eq(id), any(AceTokenCreate.class));
        verify(supervisor, times(1)).complete(eq(entry));
    }

    @Test
    public void getFilename() {
        String filename = registrar.getFilename(response);
        Assert.assertEquals(path, filename);
    }

    @Test
    public void regex() {
        Pattern pattern = Pattern.compile("\\(.*?,.*?\\)::(.*)");
        Matcher matcher = pattern.matcher(response.getName());

        boolean matches = matcher.matches();
        int groups = matcher.groupCount();
        String group = matcher.group(1);

        log.info("Matches? {}", matches);
        log.info("Groups: {}", groups);
        log.info("Group: {}", group);
        Assert.assertTrue(matches);
        Assert.assertEquals(1, groups);
        Assert.assertEquals(path, group);
    }

}