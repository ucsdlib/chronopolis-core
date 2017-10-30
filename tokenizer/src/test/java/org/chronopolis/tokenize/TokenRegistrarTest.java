package org.chronopolis.tokenize;

import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ErrorCallWrapper;
import org.chronopolis.test.support.ExceptingCallWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenRegistrarTest {
    private final Logger log = LoggerFactory.getLogger(TokenRegistrarTest.class);

    // include extraneous characters?
    private final String path = "data/path/to/file.txt";
    private final String host = "test-ims-host";
    private final String name = "test-name";
    private final String digest = "digest";
    private final String service = "test-service";
    private final String provider = "test-provider";
    private final String depositor = "test-depositor";
    private final String tokenClass = "test-token-class";
    private final Long id = 1L;
    private final Long round = 1L;
    private final Integer status = 1;

    private ManifestEntry entry;
    private AceTokenModel model;
    private TokenResponse response;
    private TokenRegistrar registrar;

    @Mock private TokenService tokens;

    @Before
    public void setup() throws DatatypeConfigurationException {
        tokens = mock(TokenService.class);

        Bag bag = new Bag();
        bag.setId(id);
        bag.setName(name);
        bag.setDepositor(depositor);
        entry = new ManifestEntry(bag, path, digest);
        entry.setCalculatedDigest(digest);

        response = new TokenResponse();
        response.setDigestProvider(provider);
        response.setDigestService(service);
        response.setName(entry.tokenName());
        response.setRoundId(round);
        response.setStatusCode(status);

        XMLGregorianCalendar calendar = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()));
        response.setTimestamp(calendar);
        response.setTokenClassName(tokenClass);

        registrar = new TokenRegistrar(tokens, entry, response, host);

        model = new AceTokenModel()
                .setCreateDate(ZonedDateTime.now())
                .setRound(round)
                .setImsService(service)
                .setProof("test-proof")
                .setFilename(path)
                .setBagId(id)
                .setId(id)
                .setAlgorithm(provider);
    }

    @Test
    public void get() throws Exception {
        CallWrapper<AceTokenModel> success = new CallWrapper<>(model);
        ExceptingCallWrapper<AceTokenModel> exception = new ExceptingCallWrapper<>(model);
        ErrorCallWrapper<AceTokenModel> error = new ErrorCallWrapper<>(model, 404, "Bag not found");
        when(tokens.createToken(eq(id), any(AceTokenModel.class))).thenReturn(exception, error, success);

        registrar.get();
        verify(tokens, times(3)).createToken(eq(id), any(AceTokenModel.class));
    }

    @Test
    public void getFilename() throws Exception {
        String filename = registrar.getFilename();
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