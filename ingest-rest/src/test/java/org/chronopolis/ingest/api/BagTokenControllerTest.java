package org.chronopolis.ingest.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeDeserializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeSerializer;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
@WebMvcTest(secure = false, controllers = BagTokenController.class)
@ContextConfiguration(classes = WebContext.class)
public class BagTokenControllerTest extends IngestTest {

    private static final String AUTHORIZED = "authorized";
    private static UserDetails admin = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_ADMIN"));

    private MockMvc mvc;
    private BagTokenController controller;

    @MockBean private SecurityContext context;
    @MockBean private Authentication authentication;

    @MockBean private SearchService<Bag, Long, BagRepository> bagService;
    @MockBean private SearchService<AceToken, Long, TokenRepository> tokenService;

    @Before
    public void setup() {
        SecurityContextHolder.setContext(context);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder()
                .deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(builder.build());
        converter.setSupportedMediaTypes(ImmutableList.of(MediaType.APPLICATION_JSON));

        controller = new BagTokenController(bagService, tokenService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    //
    // Tests
    //

    @Test
    public void testGetTokensForBag() throws Exception {
        when(tokenService.findAll(any(SearchCriteria.class), any(Pageable.class))).thenReturn(wrap(generateToken()));

        mvc.perform(
                get("/api/bags/{id}/tokens", 1L)
                        .principal(() -> "test-user"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testCreateTokenSuccess() throws Exception {
        Bag bag = new Bag("test-name", "test-depositor");
        bag.setId(1L);
        runCreateToken(generateModel(), bag, HttpStatus.CREATED);

        verify(bagService, times(1)).find(any(SearchCriteria.class));
        verify(tokenService, times(1)).save(any(AceToken.class));
    }

    @Test
    public void testCreateTokenBagNotFound() throws Exception {
        runCreateToken(generateModel(), null, HttpStatus.NOT_FOUND);
        verify(bagService, times(1)).find(any(SearchCriteria.class));
        verify(tokenService, times(0)).save(any(AceToken.class));
    }

    //
    // Helpers
    //

    private void runCreateToken(AceTokenModel model, Bag bag, HttpStatus responseStatus) throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(admin);

        mvc.perform(
                post("/api/bags/{id}/tokens", 1L)
                        .principal(() -> "test-user")
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