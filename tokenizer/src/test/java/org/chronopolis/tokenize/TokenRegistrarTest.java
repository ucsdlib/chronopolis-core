package org.chronopolis.tokenize;

import edu.umiacs.ace.ims.ws.TokenResponse;
import okhttp3.Request;
import org.chronopolis.rest.api.TokenAPI;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
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
    private TokenResponse response;
    private TokenRegistrar registrar;

    @Mock private TokenAPI tokens;

    @Before
    public void setup() throws DatatypeConfigurationException {
        tokens = mock(TokenAPI.class);

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

        registrar = new TokenRegistrar(tokens, entry, response);
    }

    @Test
    public void get() throws Exception {
        // The call wrappers should be moved to a common module
        when(tokens.createToken(eq(id), any(AceTokenModel.class))).thenReturn(new Call<AceTokenModel>() {
            @Override
            public Response<AceTokenModel> execute() throws IOException {
                return Response.success(null);
            }

            @Override
            public void enqueue(Callback<AceTokenModel> callback) {
                callback.onResponse(this, Response.success(null));
            }

            @Override
            public boolean isExecuted() {
                return false;
            }

            @Override
            public void cancel() {

            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public Call<AceTokenModel> clone() {
                return null;
            }

            @Override
            public Request request() {
                return null;
            }
        });

        registrar.get();
        verify(tokens, times(1)).createToken(eq(id), any(AceTokenModel.class));
    }

    @Test
    public void getFilename() throws Exception {
        String filename = registrar.getFilename();
        Assert.assertEquals(path, filename);
    }

    @Test
    public void regex() {
        String depositor = "ucsd-lib";
        String bag = "bb7_2015-10-07";
        String path = "data/folded_dir-withlotsofnoisy.stuff/.tagmanifest-sha256.txt";

        // Pattern pattern = Pattern.compile("(" + depositor + "," + bag + ")::");
        Pattern pattern = Pattern.compile("\\(.*?,.*?\\)::(.*)");
        Matcher matcher = pattern.matcher(response.getName());

        log.info("Matches? {}", matcher.matches());
        log.info("Groups: {}", matcher.groupCount());
        log.info("Group: {}", matcher.group(1));
    }

}