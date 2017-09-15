package org.chronopolis.tokenize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.support.PageDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeSerializer;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Beans required to spawn a TokenTask component
 *
 * @author shake
 */
@Configuration
@EnableConfigurationProperties({IngestAPIProperties.class, AceConfiguration.class})
public class TokenTaskConfiguration {

    @Bean
    public TokenService tokens(IngestAPIProperties properties) {
        return buildRetrofit(properties)
                .create(TokenService.class);
    }

    @Bean
    public IngestAPI ingest(IngestAPIProperties properties) {
        return buildRetrofit(properties)
                .create(IngestAPI.class);
    }

    private Retrofit buildRetrofit(IngestAPIProperties properties) {
        Type bagPage = new TypeToken<PageImpl<Bag>>() {}.getType();
        Type bagList = new TypeToken<List<Bag>>() {}.getType();
        Type tokenPage = new TypeToken<PageImpl<AceTokenModel>>() {}.getType();
        Type tokenList = new TypeToken<List<AceTokenModel>>() {}.getType();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(bagPage, new PageDeserializer(bagList))
                .registerTypeAdapter(tokenPage, new PageDeserializer(tokenList))
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                .create();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(properties.getUsername(), properties.getPassword()))
                .build();
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(properties.getEndpoint())
                .client(client)
                .build();
    }

    @Bean
    public TrackingThreadPoolExecutor<Bag> executor() {
        return new TrackingThreadPoolExecutor<>(4, 8, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    }

    @Bean
    public Executor executorForBatch() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Bean(destroyMethod = "close")
    public ChronopolisTokenRequestBatch batch(Executor executorForBatch, AceConfiguration configuration, TokenService tokens) {
        ChronopolisTokenRequestBatch batch = new ChronopolisTokenRequestBatch(configuration, tokens);
        executorForBatch.execute(batch);
        return batch;
    }


}
