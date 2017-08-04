package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the StorageSerializer
 *
 * Created by shake on 7/11/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class StagingStorageSerializerTest {

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private JacksonTester<StagingStorage> json;
    private String datetime = "2017-03-16T01:53:28Z";

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializerByType(StagingStorage.class, new StagingStorageSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serializer() throws IOException {
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
        storage.addFixity(fixity);

        assertThat(json.write(storage)).isEqualToJson("storage.json");
    }
}