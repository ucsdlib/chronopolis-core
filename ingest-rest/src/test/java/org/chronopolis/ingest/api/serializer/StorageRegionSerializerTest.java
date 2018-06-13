package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;
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
 * Test for the StorageRegionSerializer
 *
 * Created by shake on 7/11/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class StorageRegionSerializerTest {

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<StorageRegion> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializerByType(StorageRegion.class, new StorageRegionSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serializer() throws IOException {
        StorageRegion region = new StorageRegion();
        region.setId(2L);
        region.setNote("note");
        region.setNode(new Node("test-node", "test-node"));
        region.setStorage(ImmutableSet.of());
        region.setCapacity(250L);
        region.setDataType(DataType.BAG);
        region.setStorageType(StorageType.LOCAL);

        String datetime = "2017-03-16T01:53:28Z";
        region.setCreatedAt(ZonedDateTime.from(fmt.parse(datetime)));
        region.setUpdatedAt(ZonedDateTime.from(fmt.parse(datetime)));

        ReplicationConfig config = new ReplicationConfig();
        config.setId(1L);
        config.setPath("test-path");
        config.setServer("test-server");
        config.setUsername("test-user");
        region.setReplicationConfig(config);

        assertThat(json.write(region)).isEqualToJson("storage_region.json");
    }

}