package org.chronopolis.ingest.config;

import org.chronopolis.ingest.task.TokenThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by shake on 3/3/15.
 */
@Configuration
public class IngestConfig {

    @Bean
    public TokenThreadPoolExecutor TokenThreadPoolExecutor() {
        return new TokenThreadPoolExecutor(4, 6, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
    }

}
