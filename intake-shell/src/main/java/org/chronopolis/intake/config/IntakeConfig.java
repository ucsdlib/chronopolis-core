package org.chronopolis.intake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.support.PageDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

import java.lang.reflect.Type;
import java.util.List;

/**
 *
 * Created by shake on 4/16/14.
 */
@Configuration
public class IntakeConfig {

    private final Logger log = LoggerFactory.getLogger(IntakeConfig.class);

    @Value("${debug.retrofit:NONE}")
    String logLevel;

    @Bean
    public IngestAPI ingestAPI(IngestAPISettings apiSettings) {
        log.info("Configuring api");
        String endpoint = apiSettings.getIngestEndpoints().get(0);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(apiSettings.getIngestAPIUsername(),
                                                        apiSettings.getIngestAPIPassword()))

                .build();

        Type bagPage = new TypeToken<PageImpl<Bag>>() {}.getType();
        Type bagList = new TypeToken<List<Bag>>() {}.getType();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(bagPage, new PageDeserializer(bagList))
                .create();

        // TODO: This can timeout on long polls, see SO for potential fix
        // http://stackoverflow.com/questions/24669309/how-to-increase-timeout-for-retrofit-requests-in-robospice-android
        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                // .client()
                // .setErrorHandler(new ErrorLogger())
                // .setLogLevel(RestAdapter.LogLevel.valueOf(logLevel))
                // .setRequestInterceptor(new CredentialRequestInterceptor(
                //         apiSettings.getIngestAPIUsername(),
                //        apiSettings.getIngestAPIPassword()))
                .build();

        return adapter.create(IngestAPI.class);
    }

}
