package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.BagStatus;
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
import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus.REPLICATE;

/**
 *
 * Created by shake on 6/30/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class BagSerializerTest {

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private final String dateTimeString = "2017-06-30T19:49:12.37Z";
    // init is done in the setup
    private JacksonTester<Bag> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .serializerByType(Bag.class, new BagSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void testWriteJson() throws IOException {
        final Node node = new Node("node", "node");
        ZonedDateTime dateTime = ZonedDateTime.from(fmt.parse(dateTimeString));

        Bag b = new Bag("bag", "depositor");
        b.setId(1L);
        b.setBagStorage(createStorage());
        b.setRequiredReplications(1);
        b.setCreator("creator");
        b.setDepositor("depositor");
        b.setStatus(BagStatus.REPLICATING);
        b.setCreatedAt(dateTime);
        b.setUpdatedAt(dateTime);
        b.addDistribution(new BagDistribution().setNode(node).setStatus(REPLICATE));
        assertThat(json.write(b)).isEqualToJson("bag.json");
    }

    private StagingStorage createStorage() {
        StorageRegion region = new StorageRegion();
        region.setId(1L);

        Fixity fixity = new Fixity();
        fixity.setAlgorithm("test-algorithm")
                .setValue("test-value")
                .setCreatedAt(ZonedDateTime.from(fmt.parse(dateTimeString)));

        return new StagingStorage()
                .setRegion(region)
                .addFixity(fixity)
                .setPath("location")
                .setSize(1L)
                .setTotalFiles(1L)
                .setActive(true);
    }

}