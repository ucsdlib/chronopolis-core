package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.chronopolis.rest.models.serializers.FixityAlgorithmSerializer;
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
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Test the DataFileSerializer against a BagFile and a TokenStore
 *
 * @author shake
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class DataFileSerializerTest {

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final String dateTimeString = "2017-06-30T19:49:12.37Z";
    private final String fixityVal = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @SuppressWarnings("unused")
    private JacksonTester<DataFile> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(DataFile.class, new DataFileSerializer());
        module.addSerializer(FixityAlgorithm.class, new FixityAlgorithmSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void testSerializer() throws IOException {
        ZonedDateTime from = ZonedDateTime.from(fmt.parse(dateTimeString));
        Bag bag = new Bag();
        bag.setId(1L);

        BagFile bagFile = new BagFile();
        bagFile.setId(1L);
        bagFile.setBag(bag);
        bagFile.setSize(100L);
        bagFile.setCreatedAt(from);
        bagFile.setUpdatedAt(from);
        bagFile.setFilename("/test-filename");
        bagFile.addFixity(new Fixity(from, bagFile, fixityVal, FixityAlgorithm.SHA_256.getCanonical()));

        TokenStore tokenStore = new TokenStore();
        tokenStore.setId(1L);
        tokenStore.setBag(bag);
        tokenStore.setSize(100L);
        tokenStore.setCreatedAt(from);
        tokenStore.setUpdatedAt(from);
        tokenStore.setFilename("/test-filename");
        tokenStore.addFixity(new Fixity(from, bagFile, fixityVal, FixityAlgorithm.SHA_256.getCanonical()));

        assertThat(json.write(bagFile)).isEqualTo("file.json");
        assertThat(json.write(tokenStore)).isEqualTo("file.json");
    }


}
