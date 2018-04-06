package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
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
 * yo replication serialization tests
 *
 * todo null received fixity
 *
 * Created by shake on 6/30/17.
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = WebContext.class)
public class ReplicationSerializerTest {

    private final Depositor depositor = new Depositor().setNamespace("depositor");
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<Replication> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .serializerByType(Replication.class, new ReplicationSerializer())
                .serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .build();

        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws Exception {
        String node = "node";
        String link = "link";
        String fixity = "fixity";
        String datetime = "2017-06-30T20:24:01.311Z";
        ZonedDateTime time = ZonedDateTime.from(fmt.parse(datetime));

        Replication replication = new Replication(new Node(node, node),
                new Bag("bag", depositor),
                link,
                link);
        replication.setId(1L);
        replication.setCreatedAt(time);
        replication.setUpdatedAt(time);
        replication.setProtocol("rsync");
        replication.setReceivedTagFixity(fixity);
        replication.setReceivedTokenFixity(fixity);
        replication.setStatus(ReplicationStatus.SUCCESS);
        assertThat(json.write(replication)).isEqualToJson("replication.json");
    }

}