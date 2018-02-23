package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class DepositorSerializerTest {

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<Depositor> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .serializerByType(Depositor.class, new DepositorSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void testSerialize() throws IOException {
        final String dateTimeString = "2017-06-30T19:49:12.37Z";
        final String namespace = "test-depositor";
        final Set<DepositorContact> contacts = ImmutableSet.of(new DepositorContact()
                .setContactName("test-name")
                .setContactEmail("test-email")
                .setContactPhone("test-phone"));

        final Depositor depositor = new Depositor()
                .setContacts(contacts)
                .setNamespace(namespace)
                .setSourceOrganization("test-organization")
                .setOrganizationAddress("test-address");
        depositor.setId(1L);
        depositor.setCreatedAt(ZonedDateTime.from(fmt.parse(dateTimeString)));
        depositor.setUpdatedAt(ZonedDateTime.parse(dateTimeString));

        assertThat(json.write(depositor)).isEqualTo("depositor.json");
    }

}