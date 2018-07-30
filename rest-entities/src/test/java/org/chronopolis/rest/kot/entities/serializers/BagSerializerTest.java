package org.chronopolis.rest.kot.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.entities.BagDistribution;
import org.chronopolis.rest.kot.entities.Node;
import org.chronopolis.rest.kot.entities.depositor.Depositor;
import org.chronopolis.rest.kot.entities.storage.Fixity;
import org.chronopolis.rest.kot.entities.storage.StagingStorage;
import org.chronopolis.rest.kot.entities.storage.StorageRegion;
import org.chronopolis.rest.kot.models.enums.BagStatus;
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

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.chronopolis.rest.kot.entities.BagDistributionStatus.DISTRIBUTE;

/**
 * Created by shake on 6/30/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = SerializerContext.class)
public class BagSerializerTest {

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private final String dateTimeString = "2017-06-30T19:49:12.37Z";
    private final String NAMESPACE = "depositor";
    private final Depositor depositor = new Depositor(NAMESPACE, NAMESPACE, NAMESPACE);

    @SuppressWarnings("unused")
    private JacksonTester<Bag> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(Bag.class, new BagSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void testWriteJson() throws IOException {
        final Node node = new Node(emptySet(), emptySet(), "node", "node", true);
        ZonedDateTime dateTime = ZonedDateTime.from(fmt.parse(dateTimeString));
        Bag b = new Bag("bag", "creator", depositor, 1L, 1L, BagStatus.REPLICATING);
        b.setId(1L);
        b.setSize(1L);
        b.setTotalFiles(1L);
        b.setBagStorage(ImmutableSet.of(createStorage()));
        b.setTokenStorage(ImmutableSet.of());
        b.setCreator("creator");
        b.setStatus(BagStatus.REPLICATING);
        b.setCreatedAt(dateTime);
        b.setUpdatedAt(dateTime);
        b.setDistributions(ImmutableSet.of(new BagDistribution(b, node, DISTRIBUTE)));
        System.out.println(json.write(b));
        assertThat(json.write(b)).isEqualToJson("bag.json");
    }

    private StagingStorage createStorage() {
        StorageRegion region = new StorageRegion();
        region.setId(1L);

        StagingStorage storage = new StagingStorage();
        storage.setActive(true);
        storage.setRegion(region);
        storage.setPath("location");
        storage.setSize(1L);
        storage.setTotalFiles(1L);

        Fixity fixity = new Fixity(storage,
                ZonedDateTime.from(fmt.parse(dateTimeString)),
                "test-value", "test-algorithm");
        storage.setFixities(ImmutableSet.of(fixity));

        return storage;
    }

}