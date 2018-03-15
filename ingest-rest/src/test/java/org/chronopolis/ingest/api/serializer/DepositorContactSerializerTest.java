package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.ingest.WebContext;
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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class DepositorContactSerializerTest {

    @SuppressWarnings("unused")
    private JacksonTester<DepositorContact> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .serializerByType(DepositorContact.class, new DepositorContactSerializer())
                .build();

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws IOException {
        DepositorContact contact = new DepositorContact()
                .setContactEmail("test-email")
                .setContactName("test-name")
                .setContactPhone("test-phone");

        assertThat(json.write(contact)).isEqualTo("depositor_contact.json");
    }
}