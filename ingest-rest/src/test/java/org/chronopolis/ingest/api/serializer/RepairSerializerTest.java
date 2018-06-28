package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.fulfillment.Ace;
import org.chronopolis.rest.models.repair.AuditStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test serialization for the RepairSerializer
 *
 * todo: null from/type/strategy
 *
 * Created by shake on 6/30/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = WebContext.class)
public class RepairSerializerTest {

    private final Depositor depositor = new Depositor().setNamespace("depositor");
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<Repair> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .serializerByType(Repair.class, new RepairSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws Exception {
        String to = "to";
        String from = "from";

        // geez
        String dateTimeString = "2017-06-30T20:05:19.634Z";
        ZonedDateTime dateTime = ZonedDateTime.from(fmt.parse(dateTimeString));
        Repair repair = new Repair();
        repair.setId(1L);
        repair.setCreatedAt(dateTime);
        repair.setUpdatedAt(dateTime);
        repair.setStatus(RepairStatus.STAGING)
                .setAudit(AuditStatus.AUDITING)
                .setValidated(true)
                .setCleaned(false)
                .setReplaced(false)
                .setTo(new Node(to, to))
                .setFrom(new Node(from, from))
                .setRequester("requester")
                .setType(FulfillmentType.ACE)
                .setStrategy(new Ace().setApiKey("api-key").setUrl("url"))
                .setBag(new Bag("bag", depositor))
                .setFilesFromRequest(ImmutableSet.of("file-1", "file-2"));

        assertThat(json.write(repair)).isEqualToJson("repair.json");
    }

}