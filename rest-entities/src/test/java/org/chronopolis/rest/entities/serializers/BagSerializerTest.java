package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.BagDistributionStatus;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.collect.ImmutableSet.of;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by shake on 6/30/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = SerializerContext.class)
public class BagSerializerTest {

    private final DateTimeFormatter fmt = ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
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
        module.addSerializer(FixityAlgorithm.class, new FixityAlgorithmSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void testWriteJson() throws IOException {
        final Node node = new Node(emptySet(), "node", "node", true);
        ZonedDateTime dateTime = ZonedDateTime.from(fmt.parse(dateTimeString));
        Bag bag = new Bag("bag", "creator", depositor, 1L, 1L, BagStatus.REPLICATING);
        bag.setId(1L);
        bag.addStagingStorage(createStorage());
        bag.setDistributions(of(new BagDistribution(bag, node, BagDistributionStatus.DISTRIBUTE)));
        bag.setCreatedAt(dateTime);
        bag.setUpdatedAt(dateTime);
        assertThat(json.write(bag)).isEqualToJson("bag.json");
    }

    private StagingStorage createStorage() {
        StorageRegion region = new StorageRegion();
        region.setId(1L);

        BagFile file = new BagFile();

        StagingStorage storage = new StagingStorage();
        storage.setActive(true);
        storage.setRegion(region);
        storage.setPath("location");
        storage.setSize(1L);
        storage.setTotalFiles(1L);

        Fixity fixity = new Fixity(ZonedDateTime.from(fmt.parse(dateTimeString)),
                file,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "SHA-256");
        file.setFixities(of(fixity));
        storage.setFile(file);

        return storage;
    }

}