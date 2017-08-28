package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableList;
import org.chronopolis.rest.api.TokenAPI;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ErrorCallWrapper;
import org.chronopolis.test.support.ExceptingCallWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class HttpFilterTest {

    private final String path = "test-path";

    private HttpFilter filter;
    
    @Mock
    private TokenAPI tokens;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        filter = new HttpFilter(1L, tokens);
    }

    @Test
    public void testContains() {
        when(tokens.getBagTokens(eq(1L), anyMap())).thenReturn(wrap(ImmutableList.of(new AceTokenModel())));
        Assert.assertTrue(filter.contains(path));
    }

    @Test
    public void testNotContains() {
        when(tokens.getBagTokens(eq(1L), anyMap())).thenReturn(wrap(ImmutableList.of()));
        Assert.assertFalse(filter.contains(path));
    }

    @Test
    public void testHttpError() {
        when(tokens.getBagTokens(eq(1L), anyMap())).thenReturn(wrapError());
        Assert.assertFalse(filter.contains(path));
    }

    @Test
    public void testThrowsException() {
        when(tokens.getBagTokens(eq(1L), anyMap())).thenReturn(new ExceptingCallWrapper(new AceTokenModel()));
        Assert.assertFalse(filter.contains(path));
    }

    private CallWrapper<Page<AceTokenModel>> wrap(List<AceTokenModel> elements) {
        PageImpl<AceTokenModel> page = new PageImpl<>(elements);
        return new CallWrapper<>(page);
    }

    private ErrorCallWrapper<Page<AceTokenModel>> wrapError() {
        return new ErrorCallWrapper<>(new PageImpl<AceTokenModel>(ImmutableList.of()), 404, "Bag Not Found");
    }

}