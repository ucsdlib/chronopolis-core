package org.chronopolis.rest.entities.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.models.enums.ReplicationStatus;
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

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.chronopolis.rest.models.enums.BagStatus.DEPOSITED;

/**
 * yo replication serialization tests
 * <p>
 * todo null received fixity
 * <p>
 * Created by shake on 6/30/17.
 */
@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SerializerContext.class)
public class ReplicationSerializerTest {

    private final Depositor depositor = new Depositor("depositor", "depositor", "depositor");
    private final DateTimeFormatter fmt = ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @SuppressWarnings("unused")
    private JacksonTester<Replication> json;

    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(Replication.class, new ReplicationSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JacksonTester.initFields(this, mapper);
    }

    @Test
    public void serialize() throws Exception {
        String bagName = "bag";
        String protocol = "rsync";
        String node = "node";
        String link = "link";
        String fixity = "fixity";
        String datetime = "2017-06-30T20:24:01.311Z";
        ZonedDateTime time = ZonedDateTime.from(fmt.parse(datetime));
        Bag bag = new Bag(bagName, depositor.getNamespace(), depositor, 0L, 0L, DEPOSITED);
        Replication replication = new Replication(ReplicationStatus.SUCCESS,
                new Node(emptySet(), node, node, true),
                bag,
                link, link, protocol, fixity, fixity);
        replication.setId(1L);
        replication.setCreatedAt(time);
        replication.setUpdatedAt(time);
        assertThat(json.write(replication)).isEqualToJson("replication.json");
    }

}