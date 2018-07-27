package org.chronopolis.ingest.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.kot.entities.AceToken;
import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.entities.Replication;
import org.chronopolis.rest.kot.entities.depositor.Depositor;
import org.chronopolis.rest.kot.entities.depositor.DepositorContact;
import org.chronopolis.rest.kot.entities.repair.Repair;
import org.chronopolis.rest.kot.entities.serializers.AceTokenSerializer;
import org.chronopolis.rest.kot.entities.serializers.BagSerializer;
import org.chronopolis.rest.kot.entities.serializers.DepositorContactSerializer;
import org.chronopolis.rest.kot.entities.serializers.DepositorSerializer;
import org.chronopolis.rest.kot.entities.serializers.RepairSerializer;
import org.chronopolis.rest.kot.entities.serializers.ReplicationSerializer;
import org.chronopolis.rest.kot.entities.serializers.StagingStorageSerializer;
import org.chronopolis.rest.kot.entities.serializers.StorageRegionSerializer;
import org.chronopolis.rest.kot.entities.serializers.ZonedDateTimeSerializer;
import org.chronopolis.rest.kot.entities.storage.StagingStorage;
import org.chronopolis.rest.kot.entities.storage.StorageRegion;
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer;
import org.junit.Before;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;

/**
 *
 * @author shake
 */
@ContextConfiguration(classes = WebContext.class)
public class ControllerTest extends IngestTest {

    protected MockMvc mvc;
    protected ObjectMapper mapper;

    protected static final String REQUESTER = "requester";
    protected static final String AUTHORIZED = "authorized";
    protected static final String UNAUTHORIZED = "unauthorized";

    protected static Principal requesterPrincipal = () -> REQUESTER;
    protected static Principal authorizedPrincipal = () -> AUTHORIZED;
    protected static Principal unauthorizedPrincipal = () -> UNAUTHORIZED;

    public static UserDetails user = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_USER"));
    public static UserDetails admin = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_ADMIN"));

    // Security beans for authorizing http requests
    @MockBean protected SecurityContext context;
    @MockBean protected Authentication authentication;

    @Before
    public void setupSecurityContext() {
        SecurityContextHolder.setContext(context);
    }

    public void setupMvc(Object controller) {
        // serialization for entity -> model
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.modulesToInstall(new KotlinModule());
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.serializerByType(AceToken.class, new AceTokenSerializer());
        builder.serializerByType(Bag.class, new BagSerializer());
        builder.serializerByType(Repair.class, new RepairSerializer());
        builder.serializerByType(Depositor.class, new DepositorSerializer());
        builder.serializerByType(DepositorContact.class, new DepositorContactSerializer());
        builder.serializerByType(Replication.class, new ReplicationSerializer());
        builder.serializerByType(StagingStorage.class, new StagingStorageSerializer());
        builder.serializerByType(StorageRegion.class, new StorageRegionSerializer());
        builder.serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer());
        builder.deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        mapper = builder.build();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        converter.setSupportedMediaTypes(ImmutableList.of(MediaType.APPLICATION_JSON));

        // we could do all the controllers here which would just mean creating a few more mocks
        // which we might be able to get away with in a static context or something
        // just something to think about instead of a @Before in each test calling this
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    public void authenticateUser() {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
    }

    public void authenticateAdmin() {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(admin);
    }

    public void unauthenticated () {
    }

    public <T> Page<T> asPage(T t) {
        return new PageImpl<>(ImmutableList.of(t));
    }

    public <T> String asJson(T request) {
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

