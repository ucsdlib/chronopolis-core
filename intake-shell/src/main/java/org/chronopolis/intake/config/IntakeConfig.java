package org.chronopolis.intake.config;

import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.rest.api.IngestAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

/**
 *
 * Created by shake on 4/16/14.
 */
@Configuration
public class IntakeConfig {

    @Value("${debug.retrofit:NONE}")
    String logLevel;

    @Bean
    public IngestAPI ingestAPI(IngestAPISettings apiSettings) {
        String endpoint = apiSettings.getIngestEndpoints().get(0);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(apiSettings.getIngestAPIUsername(),
                                                       apiSettings.getIngestAPIPassword()))
                .build();

        // TODO: This can timeout on long polls, see SO for potential fix
        // http://stackoverflow.com/questions/24669309/how-to-increase-timeout-for-retrofit-requests-in-robospice-android
        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                // .setErrorHandler(new ErrorLogger())
                // .setLogLevel(RestAdapter.LogLevel.valueOf(logLevel))
                // .setRequestInterceptor(new CredentialRequestInterceptor(
                //         apiSettings.getIngestAPIUsername(),
                //        apiSettings.getIngestAPIPassword()))
                .build();

        return adapter.create(IngestAPI.class);
    }

}
