package org.chronopolis.tokenize.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.rest.api.BagService;
import org.chronopolis.rest.api.IngestApiProperties;
import org.chronopolis.rest.api.IngestGenerator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.page.SpringPage;
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.chronopolis.tokenize.BagProcessor;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.ManifestEntryDeserializer;
import org.chronopolis.tokenize.batch.ImsServiceWrapper;
import org.chronopolis.tokenize.filter.HttpFilter;
import org.chronopolis.tokenize.filter.ProcessingFilter;
import org.chronopolis.tokenize.mq.artemis.ArtemisSupervisor;
import org.chronopolis.tokenize.mq.artemis.config.ArtemisConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import retrofit2.Call;
import retrofit2.Response;

import java.time.ZonedDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Standalone runner for Tokenization through an Artemis MQ Broker
 *
 * @author shake
 */
@SpringBootApplication(scanBasePackageClasses = ArtemisConfiguration.class,
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties({
        AceConfiguration.class,
        IngestApiProperties.class,
        BagStagingProperties.class})
public class MqApplication implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(MqApplication.class);

    private final BagService bags;
    private final TokenService tokens;
    private final ServerLocator locator;
    private final AceConfiguration aceConfiguration;
    private final IngestApiProperties apiProperties;
    private final BagStagingProperties staging;

    private final TrackingThreadPoolExecutor<Bag> bagExecutor;

    @Autowired
    public MqApplication(ServerLocator serverLocator,
                         AceConfiguration aceConfiguration,
                         IngestApiProperties ingestAPIProperties,
                         BagStagingProperties bagStagingProperties) {
        IngestGenerator generator = new IngestGenerator(ingestAPIProperties);
        this.bags = generator.bags();
        this.tokens = generator.tokens();
        this.locator = serverLocator;
        this.aceConfiguration = aceConfiguration;
        this.apiProperties = ingestAPIProperties;
        this.staging = bagStagingProperties;
        this.bagExecutor = new TrackingThreadPoolExecutor<>(4, 8, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(MqApplication.class));
    }

    @Override
    public void run(String... args) throws Exception {
        TimeUnit.SECONDS.sleep(5);
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addDeserializer(ManifestEntry.class, new ManifestEntryDeserializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        mapper.registerModule(module);

        ImsServiceWrapper ims = new ImsServiceWrapper(aceConfiguration.getIms());
        ArtemisSupervisor supervisor = new ArtemisSupervisor(locator, mapper, tokens, ims);
        ProcessingFilter processingFilter = new ProcessingFilter(supervisor);

        ImmutableMap<String, String> params = ImmutableMap.of(
                "creator", apiProperties.getUsername(),
                "status", BagStatus.DEPOSITED.toString(),
                "region_id", staging.getPosix().getId().toString());

        Call<SpringPage<Bag>> allBags = bags.get(params);
        Response<SpringPage<Bag>> response = allBags.execute();
        if (response.isSuccessful()
                && response.body() != null
                && response.body().iterator().hasNext()) {
            log.info("Starting tokenization on {} bags", response.body().getNumberOfElements());
            for (Bag bag : response.body()) {
                HttpFilter httpFilter = new HttpFilter(bag.getId(), tokens);
                ImmutableList<Predicate<ManifestEntry>> predicates = ImmutableList.of(
                        processingFilter,
                        httpFilter);

                BagProcessor processor = new BagProcessor(bag, predicates, staging, supervisor);
                bagExecutor.submitIfAvailable(processor, bag);
            }
        }

        TimeUnit.SECONDS.sleep(30);

        // There's a brief period of time where the BagProcessors finished (activeCount == 0) and
        // it does not look like we are processing (messages are being worked on). I'm not quite
        // sure how to handle this yet but it's something to be aware of. Maybe for now requiring
        // x number of attempts to be done before shutting down. Checking consumerExecutor doesn't
        // help because they will always be running... unless we add logic there which we probably
        // want anyways
        while (bagExecutor.getActiveCount() > 0 || supervisor.isProcessing()) {
            log.debug("Sleeping...");
            TimeUnit.SECONDS.sleep(5);
        }

        supervisor.close();

        bagExecutor.shutdownNow();
        bagExecutor.awaitTermination(30, TimeUnit.SECONDS);
    }
}
