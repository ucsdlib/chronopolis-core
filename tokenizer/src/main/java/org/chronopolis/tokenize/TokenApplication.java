package org.chronopolis.tokenize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.rest.api.BagService;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.chronopolis.tokenize.config.TokenTaskConfiguration;
import org.chronopolis.tokenize.filter.HttpFilter;
import org.chronopolis.tokenize.filter.ProcessingFilter;
import org.chronopolis.tokenize.registrar.HttpTokenRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@SpringBootApplication(scanBasePackageClasses = TokenTaskConfiguration.class,
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties({AceConfiguration.class,
        IngestAPIProperties.class,
        BagStagingProperties.class})
public class TokenApplication implements CommandLineRunner {
    private final Logger log = LoggerFactory.getLogger(TokenTaskConfiguration.TOKENIZER_LOG_NAME);

    private final TokenService tokens;
    private final BagService bagService;
    private final HttpTokenRegistrar registrar;
    private final TokenWorkSupervisor supervisor;
    private final BagStagingProperties properties;
    private final IngestAPIProperties ingestProperties;
    private final ChronopolisTokenRequestBatch batch;

    private final Executor executorForBatch;
    private final TrackingThreadPoolExecutor<Bag> executor;

    @Autowired
    public TokenApplication(ServiceGenerator generator,
                            HttpTokenRegistrar registrar,
                            TokenWorkSupervisor supervisor,
                            BagStagingProperties properties,
                            IngestAPIProperties ingestProperties,
                            ChronopolisTokenRequestBatch batch,
                            Executor executorForBatch,
                            TrackingThreadPoolExecutor<Bag> executor) {
        this.tokens = generator.tokens();
        this.bagService = generator.bags();
        this.registrar = registrar;
        this.supervisor = supervisor;
        this.properties = properties;
        this.ingestProperties = ingestProperties;
        this.batch = batch;
        this.executorForBatch = executorForBatch;
        this.executor = executor;
    }

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(TokenApplication.class));
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run(String... strings) throws Exception {
        executorForBatch.execute(batch);
        executorForBatch.execute(registrar);

        ImmutableMap<String, Object> DEFAULT_QUERY = ImmutableMap.of(
                "creator", ingestProperties.getUsername(),
                "status", BagStatus.DEPOSITED,
                "region_id", properties.getPosix().getId());

        Call<PageImpl<Bag>> bags = bagService.get(DEFAULT_QUERY);
        try {
            Response<PageImpl<Bag>> response = bags.execute();

            while (response.isSuccessful() && response.body().getNumberOfElements() > 0) {
                log.debug("Found {} bags for tokenization", response.body().getSize());
                executeBatch(response.body());

                // polling updated pages can be a problem if bag states are updated as we go on
                // maybe some changes to the ingest api can help alleviate some of the changes here
                bags = bagService.get(DEFAULT_QUERY);
                response = bags.execute();
            }
        } catch (IOException e) {
            log.error("Error communicating with the ingest server", e);
        }

        batch.close();
        registrar.close();
        executor.shutdown();
    }

    /**
     * Process a batch, waiting for all bags in the current batch to finish tokenizing
     *
     * @param body the response body
     * @throws InterruptedException if an interruption occurs
     */
    private void executeBatch(PageImpl<Bag> body) throws InterruptedException {
        for (Bag bag : body) {
            ImmutableList<Predicate<ManifestEntry>> predicates =
                    ImmutableList.of(new ProcessingFilter(supervisor),
                            new HttpFilter(bag.getId(), tokens));

            executor.submitIfAvailable(
                    new BagProcessor(bag, predicates, properties, supervisor),
                    bag);
        }

        while (executor.getActiveCount() > 0 || supervisor.isProcessing()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
