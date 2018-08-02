package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableList;
import org.chronopolis.rest.kot.api.TokenService;
import org.chronopolis.rest.kot.models.AceToken;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ErrorCallWrapper;
import org.chronopolis.test.support.ExceptingCallWrapper;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;

import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class HttpFilterTest {


    private HttpFilter filter;
    private ManifestEntry entry;

    @Mock
    private TokenService tokens;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        String path = "test-path";
        String digest = "test-path";

        filter = new HttpFilter(1L, tokens);
        entry = new ManifestEntry(null, path, digest);
    }

    @Test
    public void testContains() {
        AceToken token = new AceToken(1L, 1L, 1L, "p", "h", "f", "a", "s", ZonedDateTime.now());
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(new CallWrapper<>(ImmutableList.of(token)));
        Assert.assertFalse(filter.test(entry));
    }

    @Test
    public void testNotContains() {
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(new CallWrapper<>(ImmutableList.of()));
        Assert.assertTrue(filter.test(entry));
    }

    @Test
    public void testHttpError() {
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(new ErrorCallWrapper<>(ImmutableList.of(), 404, "Bag Not Found"));
        Assert.assertFalse(filter.test(entry));
    }

    @Test
    public void testThrowsException() {
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(new ExceptingCallWrapper<>(ImmutableList.of()));
        Assert.assertFalse(filter.test(entry));
    }

}