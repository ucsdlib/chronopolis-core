package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test an AceTokenSerializer from Java
 *
 * @author shake
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class AceTokenSerializerTest {

    // I'll have to make a note of this elsewhere but when reading in Tokens we don't
    // want to impose any type of Zone on the String and instead use what the token offers
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @SuppressWarnings("unused")
    private JacksonTester<AceToken> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(AceToken.class, new AceTokenSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws Exception {
        // Use a ZonedDateTime with a TimeZone which is not UTC
        ZonedDateTime from = ZonedDateTime.from(fmt.parse("2013-02-11T13:36:40.000-05:00"));
        Date date = Date.from(from.toInstant());

        final Long id = 1L;
        final Long round = 100L;
        final String algorithm = "test-algorithm";
        final String ims = "test-ims-service";
        final String host = "test-ims-host";
        final String proof = "test-proof";
        final String filename = "test-filename";
        final BagFile bagFile = new BagFile();
        bagFile.setId(id);
        bagFile.setFilename(filename);
        bagFile.setCreatedAt(ZonedDateTime.now());
        bagFile.setSize(id);
        bagFile.setFixities(new HashSet<>());
        AceToken token = new AceToken(proof, round, ims, algorithm, host, date, bagFile);
        token.setId(id);
        token.setBag(genBag());
        assertThat(json.write(token)).isEqualToJson("token.json");
    }

    private BagFile genBagFile() {
        return null;
    }

    private Bag genBag() {
        Bag b = new Bag();
        b.setId(1L);
        return b;
    }

}
