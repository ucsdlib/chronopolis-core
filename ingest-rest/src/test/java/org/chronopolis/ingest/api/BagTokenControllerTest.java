package org.chronopolis.ingest.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.ingest.repository.dao.TokenDao;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the BagTokenController
 * <p>
 * - Remove magic vals
 * - Check json output
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BagTokenController.class)
@ContextConfiguration(classes = WebContext.class)
public class BagTokenControllerTest extends ControllerTest {

    private static final String AUTHORIZED = "authorized";
    private static UserDetails admin = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_ADMIN"));

    private BagTokenController controller;

    @MockBean private TokenDao dao;
    @MockBean private JPAQueryFactory factory;

    @Before
    public void setup() {
        controller = new BagTokenController(dao);
        setupMvc(controller);
    }

    //
    // Tests
    //

    @Test
    public void testGetTokensForBag() throws Exception {
        when(dao.findPage(eq(QAceToken.aceToken), any(Paged.class)))
                .thenReturn(wrap(generateToken()));

        mvc.perform(
                get("/api/bags/{id}/tokens", 1L)
                        .principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testCreateTokenSuccess() throws Exception {
        Bag bag = generateBag();
        AceTokenCreate model = generateModel();
        ResponseEntity<AceToken> response =
                ResponseEntity.status(HttpStatus.CREATED).body(generateToken());
        // for whatever reason this gets weird when checking equality against the model
        // not like that matters because this tests literally nothing except serialization I guess
        when(dao.createToken(any(Principal.class), eq(bag.getId()), any()))
                .thenReturn(response);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(admin);

        mvc.perform(
                post("/api/bags/{id}/tokens", bag.getId())
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(model)))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.CREATED.value()));
    }

    //
    // Helpers
    //

    private String json(AceTokenCreate model) throws JsonProcessingException {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        final Logger log = LoggerFactory.getLogger(BagTokenControllerTest.class);
        log.info("{}", mapper.writeValueAsString(model));
        return mapper.writeValueAsString(model);
    }

    private AceTokenCreate generateModel() {
        return new AceTokenCreate(1L, 1L, ZonedDateTime.now(), "test-proof", "test-ims-host",
                "data/test-file", "test-algorithm", "test-ims-service");
    }

    private Bag generateBag() {
        Bag bag = new Bag("test-name", "namespace", DEPOSITOR, 1L, 1L, BagStatus.DEPOSITED);
        bag.setId(1L);
        return bag;
    }

    // These are pulled from the TokenControllerTest, since we're doing simple operations at the moment that's ok
    // but we'll probably want a better way to do this
    @SuppressWarnings("Duplicates")
    private AceToken generateToken() {
        BagFile file = new BagFile();
        file.setFilename("test-filename");

        AceToken token = new AceToken("test-proof", 100L, "test-ims-host",
                "test-ims", "test-algorithm", new Date(), file);
        token.setId(1L);
        token.setBag(generateBag());
        return token;
    }

    // put this in a super class from which all *Controller test can extend yayaya
    private <T> Page<T> wrap(T t) {
        return new PageImpl<>(ImmutableList.of(t));
    }

}