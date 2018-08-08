package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.DepositorContact;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class DepositorSerializerTest {

    @SuppressWarnings("unused")
    private JacksonTester<Depositor> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(Depositor.class, new DepositorSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void testSerialize() throws IOException {
        final String dateTimeString = "2017-06-30T19:49:12.37Z";
        final String namespace = "test-depositor";
        final Set<DepositorContact> contacts = ImmutableSet.of(
                new DepositorContact("test-name", "test-phone", "test-email"));

        final Depositor depositor = new Depositor(namespace, "test-organization", "test-address");
        depositor.setId(1L);
        depositor.setContacts(contacts);
        depositor.setNodeDistributions(Collections.emptySet());
        depositor.setCreatedAt(ZonedDateTime.parse(dateTimeString));
        depositor.setUpdatedAt(ZonedDateTime.parse(dateTimeString));

        assertThat(json.write(depositor)).isEqualTo("depositor.json");
    }

}