package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class AceTokenSerializerTest {

    private final Depositor depositor = new Depositor().setNamespace("depositor");
    // I'll have to make a note of this elsewhere but when reading in Tokens we don't
    // want to impose any type of Zone on the String and instead use what the token offers
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @SuppressWarnings("unused")
    private JacksonTester<AceToken> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .serializerByType(AceToken.class, new AceTokenSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws Exception {
        // Use a ZonedDateTime with a TimeZone which is not UTC
        ZonedDateTime from = ZonedDateTime.from(fmt.parse("2013-02-11T13:36:40.000-05:00"));
        Date date = Date.from(from.toInstant());

        final Long round = 100L;
        final String algorithm = "test-algorithm";
        final String ims = "test-ims-service";
        final String host = "test-ims-host";
        final String proof = "test-proof";
        final String filename = "test-filename";
        AceToken token = new AceToken(genBag(), date, filename, proof, host, ims, algorithm, round);
        assertThat(json.write(token)).isEqualToJson("token.json");
    }

    private Bag genBag() {
        Bag bag = new Bag("test-name", depositor);
        bag.setId(1L);
        return bag;
    }

}