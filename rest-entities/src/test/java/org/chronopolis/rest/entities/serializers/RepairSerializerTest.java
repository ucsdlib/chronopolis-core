package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.repair.Ace;
import org.chronopolis.rest.entities.repair.Repair;
import org.chronopolis.rest.models.enums.AuditStatus;
import org.chronopolis.rest.models.enums.FulfillmentType;
import org.chronopolis.rest.models.enums.RepairStatus;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.chronopolis.rest.models.enums.BagStatus.DEPOSITED;

/**
 * Test serialization for the RepairSerializer
 *
 * todo: null from/type/strategy
 *
 * Created by shake on 6/30/17.
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class RepairSerializerTest {

    private final Depositor depositor = new Depositor("depositor", "depositor", "depositor");
    private final DateTimeFormatter fmt = ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<Repair> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(Repair.class, new RepairSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws Exception {
        String to = "to";
        String from = "from";
        Node toNode = new Node(emptySet(), to, to, true);
        Node fromNode = new Node(emptySet(), from, from, true);
        Bag bag = new Bag("bag", depositor.getNamespace(), depositor, 0L, 0L, DEPOSITED);
        bag.setDistributions(emptySet());

        // geez
        String dateTimeString = "2017-06-30T20:05:19.634Z";
        ZonedDateTime dateTime = ZonedDateTime.from(fmt.parse(dateTimeString));
        Repair repair = new Repair(bag, toNode, fromNode,
                RepairStatus.STAGING, AuditStatus.AUDITING,
                FulfillmentType.ACE, new Ace("api-key", "url"),
                "requester",false, false, true);
        repair.setFiles(new HashSet<>());
        repair.addFilesFromRequest(ImmutableSet.of("file-1", "file-2"));
        repair.setId(1L);
        repair.setCreatedAt(dateTime);
        repair.setUpdatedAt(dateTime);
        assertThat(json.write(repair)).isEqualToJson("repair.json");
    }

}