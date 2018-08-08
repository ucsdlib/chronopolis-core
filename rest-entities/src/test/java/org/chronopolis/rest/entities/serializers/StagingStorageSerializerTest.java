package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the StorageSerializer
 * <p>
 * Created by shake on 7/11/17.
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class StagingStorageSerializerTest {

    private final DateTimeFormatter fmt = ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<StagingStorage> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addSerializer(StagingStorage.class, new StagingStorageSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serializer() throws IOException {
        String datetime = "2017-03-16T01:53:28Z";
        ZonedDateTime zdt = ZonedDateTime.from(fmt.parse(datetime));
        StagingStorage storage = new StagingStorage();
        storage.setId(1L);
        storage.setUpdatedAt(zdt);
        storage.setCreatedAt(zdt);
        storage.setActive(true);
        // storage.setChecksum("test-checksum");
        storage.setPath("test-path");
        storage.setSize(100L);
        storage.setTotalFiles(10L);

        // We only need the id for the region
        StorageRegion region = new StorageRegion();
        region.setId(2L);
        storage.setRegion(region);

        // And a single Fixity entry
        Fixity fixity = new Fixity();
        fixity.setId(1L);
        fixity.setAlgorithm("test-algorithm");
        fixity.setValue("test-value");
        fixity.setCreatedAt(zdt);
        storage.setFixities(ImmutableSet.of(fixity));

        assertThat(json.write(storage)).isEqualToJson("storage.json");
    }
}