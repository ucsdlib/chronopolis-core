package org.chronopolis.replicate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.AceSettings;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.common.util.URIUtil;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.rest.api.ErrorLogger;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.support.PageDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the beans used by the replication-shell
 *
 * Created by shake on 4/16/14.
 */
@SuppressWarnings("ALL")
@Configuration
public class ReplicationConfig {
    public final Logger log = LoggerFactory.getLogger(ReplicationConfig.class);

    @Value("${debug.retrofit:NONE}")
    String retrofitLogLevel;

    @Value("${ace.timeout:5}")
    Long timeout;

    /**
     * Logger to capture why errors happened in Retrofit
     *
     * @return
     */
    @Bean
    ErrorLogger logger() {
        return new ErrorLogger();
    }

    /**
     * Retrofit adapter for interacting with the ACE REST API
     *
     * @param aceSettings - Settings to connect to ACE with
     * @return
     */
    @Bean
    AceService aceService(AceSettings aceSettings) {
        // Next build the retrofit adapter
        String endpoint = URIUtil.buildAceUri(aceSettings.getAmHost(),
                aceSettings.getAmPort(),
                aceSettings.getAmPath()).toString();

        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }

        // TODO: Test
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(aceSettings.getAmHost())
                .port(aceSettings.getAmPort())
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(aceSettings.getAmUser(), aceSettings.getAmPassword()))
                .readTimeout(timeout, TimeUnit.MINUTES)
                .writeTimeout(timeout, TimeUnit.MINUTES)
                .build();

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                // .setErrorHandler(logger())
                // .setLogLevel(Retrofit.LogLevel.valueOf(retrofitLogLevel))
                .build();

        return restAdapter.create(AceService.class);
    }

    /**
     * Retrofit adapter for interacting with the ingest-server REST API
     *
     * @param apiSettings - Settings to connect to the ingest-server with
     * @return
     */
    @Bean
    IngestAPI ingestAPI(IngestAPISettings apiSettings) {
        // TODO: Create a list of endpoints
        String endpoint = apiSettings.getIngestEndpoints().get(0);

        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        for (String s : chain.request().headers().names()) {
                            log.info(chain.request().headers().get(s));
                        }
                        return chain.proceed(chain.request());
                    }
                })
                .addNetworkInterceptor(new OkBasicInterceptor(apiSettings.getIngestAPIUsername(),
                        apiSettings.getIngestAPIPassword()))
                .build();

        Type bagPage = new TypeToken<PageImpl<Bag>>() {}.getType();
        Type bagList = new TypeToken<List<Bag>>() {}.getType();
        Type replPage = new TypeToken<PageImpl<Replication>>() {}.getType();
        Type replList = new TypeToken<List<Replication>>() {}.getType();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(bagPage, new PageDeserializer(bagList))
                .registerTypeAdapter(replPage, new PageDeserializer(replList))
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                .create();

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                // .setErrorHandler(logger())
                // .setLogLevel(Retrofit.LogLevel.valueOf(retrofitLogLevel))
                // .setLogLevel(Retrofit.LogLevel.FULL)
                .build();

        return adapter.create(IngestAPI.class);
    }

    @Bean
    Submitter submitter(MailUtil mail, AceService ace, IngestAPI ingest, ReplicationSettings settings) {
        return new Submitter(mail, ace, ingest, settings, io(), http());
    }

    @Bean
    ThreadPoolExecutor http() {
        return new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    @Bean
    ThreadPoolExecutor io() {
        return new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    /**
     * Class to send email notifications regarding replications
     *
     * @param smtpSettings - The settings to use for smtp messages
     * @return
     */
    @Bean
    MailUtil mailUtil(SMTPSettings smtpSettings) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(smtpSettings.getFrom());
        mailUtil.setSmtpTo(smtpSettings.getTo());
        mailUtil.setSmtpHost(smtpSettings.getHost());
        mailUtil.setSmtpSend(smtpSettings.getSend());
        return mailUtil;
    }

}
