package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashSet;

import static org.chronopolis.rest.models.enums.BagStatus.DEPOSITED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for the TokenController
 *
 * @author shake
 */
@RunWith(SpringRunner.class)
@WebMvcTest(secure = false, controllers = TokenController.class)
@ContextConfiguration(classes = WebContext.class)
public class TokenControllerTest {

    @MockBean private PagedDao dao;
    @Autowired private MockMvc mvc;

    @Test
    public void getTokens() throws Exception {
        when(dao.findPage(any(), any(Paged.class)))
                .thenReturn(wrap(generateToken()));

        mvc.perform(get("/api/tokens").principal(() -> "test-principal"))
                // .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getToken() throws Exception {
        when(dao.findOne(any(), any(Predicate.class)))
                .thenReturn(generateToken());

        mvc.perform(get("/api/tokens/{id}", 1L).principal(() -> "test-principal"))
                // .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getTokenNotFound() throws Exception {
        when(dao.findOne(any(), any(Predicate.class)))
                .thenReturn(null);

        mvc.perform(get("/api/tokens/{id}", 1L).principal(() -> "test-principal"))
                // .andDo(print())
                .andExpect(status().isNotFound());
    }

    private AceToken generateToken() {
        Depositor depositor = new Depositor("depositor", "depositor", "depositor");
        depositor.setContacts(new HashSet<>());
        depositor.setNodeDistributions(new HashSet<>());
        Bag bag = new Bag("test-name", depositor.getNamespace(), depositor, 1L, 1L, DEPOSITED);
        bag.setId(1L);
        BagFile file = new BagFile();
        file.setFilename("test-filename");
        AceToken token = new AceToken("test-proof",
                100L,
                "test-ims",
                "test-algorithm",
                "test-ims-host",
                new Date(),
                bag,
                file);
        token.setId(1L);
        return token;
    }

    // put this in a super class from which all *Controller test can extend yayaya
    private <T> Page<T> wrap(T t) {
        return new PageImpl<>(ImmutableList.of(t));
    }
}