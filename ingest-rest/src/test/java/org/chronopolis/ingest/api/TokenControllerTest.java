package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for the TokenController
 *
 * todo: AceToken -> AceTokenModel during serialization
 *
 * @author shake
 */
@RunWith(SpringRunner.class)
@WebMvcTest(secure = false, controllers = TokenController.class)
@ContextConfiguration(classes = WebContext.class)
public class TokenControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SearchService<AceToken, Long, TokenRepository> tokens;

    @Test
    public void getTokens() throws Exception {
        when(tokens.findAll(any(SearchCriteria.class), any(Pageable.class)))
                .thenReturn(wrap(generateToken()));

        mvc.perform(get("/api/tokens").principal(() -> "test-principal"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getToken() throws Exception {
        when(tokens.find(any(SearchCriteria.class)))
                .thenReturn(generateToken());

        mvc.perform(get("/api/tokens/{id}", 1L).principal(() -> "test-principal"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getTokenNotFound() throws Exception {
        when(tokens.find(any(SearchCriteria.class)))
                .thenReturn(null);

        mvc.perform(get("/api/tokens/{id}", 1L).principal(() -> "test-principal"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private AceToken generateToken() {
        Bag bag = new Bag("test-name", "test-depositor");
        bag.setId(1L);
        AceToken token = new AceToken(bag,
                new Date(),
                "test-filename",
                "test-proof",
                "test-ims",
                "test-algorithm",
                100L);
        token.setId(1L);
        return token;
    }

    // put this in a super class from which all *Controller test can extend yayaya
    private <T> Page<T> wrap(T t) {
        return new PageImpl<>(ImmutableList.of(t));
    }
}