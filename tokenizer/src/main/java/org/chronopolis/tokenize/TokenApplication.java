package org.chronopolis.tokenize;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.api.BagService;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.chronopolis.tokenize.config.TokenTaskConfiguration;
import org.chronopolis.tokenize.filter.HttpFilter;
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
import java.util.concurrent.TimeUnit;

@SpringBootApplication(scanBasePackageClasses = TokenTaskConfiguration.class,
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties({AceConfiguration.class, IngestAPIProperties.class, BagStagingProperties.class})
public class TokenApplication implements CommandLineRunner {
    private final Logger log = LoggerFactory.getLogger(TokenApplication.class);

    private final TokenService tokens;
    private final BagService bagService;
    private final BagStagingProperties properties;
    private final ChronopolisTokenRequestBatch batch;
    private final TrackingThreadPoolExecutor<Bag> executor;

    @Autowired
    public TokenApplication(TokenService tokens,
                            ServiceGenerator generator,
                            BagStagingProperties properties,
                            ChronopolisTokenRequestBatch batch,
                            TrackingThreadPoolExecutor<Bag> executor) {
        this.tokens = tokens;
        this.bagService = generator.bags();
        this.properties = properties;
        this.batch = batch;
        this.executor = executor;
    }


    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(TokenApplication.class));
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run(String... strings) throws Exception {
        Call<PageImpl<Bag>> bags = bagService.get(ImmutableMap.of(
                "status", BagStatus.DEPOSITED,
                "region_id", properties.getPosix().getId()));
        try {
            Response<PageImpl<Bag>> response = bags.execute();
            if (response.isSuccessful()) {
                log.debug("Found {} bags for tokenization", response.body().getSize());
                for (Bag bag : response.body()) {
                    Filter<String> filter = new HttpFilter(bag.getId(), tokens);
                    executor.submitIfAvailable(new BagProcessor(bag, filter, properties, batch), bag);
                }
            }
        } catch (IOException e) {
            log.error("Error communicating with the ingest server", e);
        }

        while(executor.getActiveCount() > 0) {
            TimeUnit.SECONDS.sleep(5);
        }
    }
}
