package org.chronopolis.ingest.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.api.serializer.StorageRegionSerializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeDeserializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeSerializer;
import org.chronopolis.ingest.models.RegionCreate;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.storage.StorageType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * todo: tests for non-successful responses
 *
 * Created by shake on 7/11/17.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(secure = false, controllers = StorageController.class)
@ContextConfiguration(classes = WebContext.class)
public class StorageControllerTest {

    private MockMvc mvc;
    private StorageController controller;

    // Constructor params
    @MockBean private NodeRepository nodes;
    @MockBean private SearchService<StorageRegion, Long, StorageRegionRepository> service;

    // Auth handling
    @MockBean private SecurityContext context;
    @MockBean private Authentication authentication;

    @Before
    public void setup() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.serializerByType(StorageRegion.class, new StorageRegionSerializer());
        builder.serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer());
        builder.deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(builder.build());
        converter.setSupportedMediaTypes(ImmutableList.of(MediaType.APPLICATION_JSON));

        controller = new StorageController(nodes, service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    public void getRegion() throws Exception {
        // todo: return actual StorageRegion and check json
        when(service.find(any(SearchCriteria.class))).thenReturn(null);

        mvc.perform(get("/api/storage/{id}", 1L)
                .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getRegions() throws Exception {
        mvc.perform(get("/api/storage")
                .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void createRegion() throws Exception {
        String node = "test-node";
        setupAuth(new User(node, node, ImmutableList.of(() -> "ROLE_USER")));

        RegionCreate request = new RegionCreate();
        request.setCapacity(1000L)
                .setNode(node)
                .setStorageType(StorageType.LOCAL)
                .setReplicationPath("/test-path")
                .setReplicationServer("test-server")
                .setReplicationUser("test-user");

        when(nodes.findByUsername(eq(node))).thenReturn(new Node(node, node));
        mvc.perform(post("/api/storage")
                .principal(() -> node)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJson(request)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    private void setupAuth(UserDetails details) {
        SecurityContextHolder.setContext(context);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
    }

    private <T> String asJson(T request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}