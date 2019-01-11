package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.DataType;
import org.chronopolis.rest.models.enums.StorageType;
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
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the StorageRegionSerializer
 * <p>
 * Created by shake on 7/11/17.
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class StorageRegionSerializerTest {

    private final DateTimeFormatter fmt = ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<StorageRegion> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(StorageRegion.class, new StorageRegionSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serializer() throws IOException {
        StorageRegion region = new StorageRegion();
        region.setId(2L);
        region.setNote("note");
        region.setNode(new Node(emptySet(), "test-node", "test-node", true));
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