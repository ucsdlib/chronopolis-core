package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableList;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ErrorCallWrapper;
import org.chronopolis.test.support.ExceptingCallWrapper;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;

import java.util.List;

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
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(wrap(ImmutableList.of(new AceTokenModel())));
        Assert.assertFalse(filter.test(entry));
    }

    @Test
    public void testNotContains() {
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(wrap(ImmutableList.of()));
        Assert.assertTrue(filter.test(entry));
    }

    @Test
    public void testHttpError() {
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class))).thenReturn(wrapError());
        Assert.assertFalse(filter.test(entry));
    }

    @Test
    public void testThrowsException() {
        ExceptingCallWrapper<PageImpl<AceTokenModel>> call =
                new ExceptingCallWrapper<>(new PageImpl<>(ImmutableList.of()));
        when(tokens.getBagTokens(eq(1L), anyMapOf(String.class, String.class)))
                .thenReturn(call);
        Assert.assertFalse(filter.test(entry));
    }

    private Call<PageImpl<AceTokenModel>> wrap(List<AceTokenModel> elements) {
        PageImpl<AceTokenModel> page = new PageImpl<>(elements);
        return new CallWrapper<>(page);
    }

    private ErrorCallWrapper<PageImpl<AceTokenModel>> wrapError() {
        return new ErrorCallWrapper<>(
                new PageImpl<>(ImmutableList.of()), 404, "Bag Not Found");
    }

}