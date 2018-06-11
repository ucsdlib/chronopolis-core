package org.chronopolis.ingest.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeSerializer;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.models.AceTokenModel;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    private final Depositor depositor = new Depositor();
    private static final String AUTHORIZED = "authorized";
    private static UserDetails admin = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_ADMIN"));

    private BagTokenController controller;

    @MockBean private PagedDAO dao;
    @MockBean private JPAQueryFactory factory;
    // @MockBean private BagService bagService;
    // @MockBean private SearchService<AceToken, Long, TokenRepository> tokenService;

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
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testCreateTokenSuccess() throws Exception {
        Bag bag = new Bag("test-name", depositor);
        bag.setId(1L);
        runCreateToken(generateModel(), bag, 0L, HttpStatus.CREATED);

        verify(dao, times(1)).save(any(AceToken.class));
    }

    @Test
    public void testCreateTokenBagNotFound() throws Exception {
        runCreateToken(generateModel(), null, 0L, HttpStatus.NOT_FOUND);
        verify(dao, times(0)).save(any(AceToken.class));
    }

    @Test
    public void testCreateTokenBadRequest() throws Exception {
        Bag bag = new Bag("test-name", depositor);
        bag.setId(1L);
        AceTokenModel model = generateModel();
        model.setFilename(null);

        runCreateToken(model, bag, 1L, HttpStatus.BAD_REQUEST);
        verify(dao, times(0)).save(any(AceToken.class));
    }


    //
    // Helpers
    //

    private void runCreateToken(AceTokenModel model,
                                Bag bag,
                                long tokenCount,
                                HttpStatus responseStatus) throws Exception {
        JPAQuery query = mock(JPAQuery.class);
        when(dao.getJPAQueryFactory()).thenReturn(factory);
        when(factory.from(QBag.bag)).thenReturn(query);

        // pretty rancid
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.select(QBag.bag)).thenReturn(query);
        when(query.fetchOne()).thenReturn(bag);

        when(factory.select(eq(QAceToken.aceToken.id))).thenReturn(query);
        when(query.from(eq(QAceToken.aceToken))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.fetchCount()).thenReturn(tokenCount);

        // when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(admin);

        mvc.perform(
                post("/api/bags/{id}/tokens", 1L)
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(model)))
                .andDo(print())
                .andExpect(status().is(responseStatus.value()));
    }

    private String json(AceTokenModel model) throws JsonProcessingException {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        final Logger log = LoggerFactory.getLogger(BagTokenControllerTest.class);
        log.info("{}", mapper.writeValueAsString(model));
        return mapper.writeValueAsString(model);
    }

    private AceTokenModel generateModel() {
        AceTokenModel model = new AceTokenModel();
        model.setImsHost("test-ims-host");
        model.setImsService("test-ims-service");
        model.setAlgorithm("test-algorithm");
        model.setRound(1L);
        model.setCreateDate(ZonedDateTime.now());
        model.setFilename("data/test-file");
        model.setProof("test-proof");
        return model;
    }

    // These are pulled from the TokenControllerTest, since we're doing simple operations at the moment that's ok
    // but we'll probably want a better way to do this
    @SuppressWarnings("Duplicates")
    private AceToken generateToken() {
        Bag bag = new Bag("test-name", depositor);
        bag.setId(1L);
        AceToken token = new AceToken(bag,
                new Date(),
                "test-filename",
                "test-proof",
                "test-ims-host",
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