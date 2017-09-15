package org.chronopolis.rest.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.repair.Repair;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.chronopolis.rest.models.storage.StorageRegion;
import org.chronopolis.rest.support.OkBasicInterceptor;
import org.chronopolis.rest.support.PageDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeSerializer;
import org.springframework.data.domain.PageImpl;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Generate retrofit things for all chronopolis ingest services
 *
 * @author shake
 */
public class IngestGenerator implements ServiceGenerator {

    private final IngestAPIProperties properties;

    public IngestGenerator(IngestAPIProperties properties) {
        this.properties = properties;
    }

    @Override
    public BagService bags() {
        return retrofit(new TypeToken<PageImpl<Bag>>() {}.getType(),
                        new TypeToken<List<Bag>>() {}.getType())
                .create(BagService.class);
    }

    @Override
    public TokenService tokens() {
        return retrofit(new TypeToken<PageImpl<AceTokenModel>>() {}.getType(),
                        new TypeToken<List<AceTokenModel>>() {}.getType())
                .create(TokenService.class);
    }

    @Override
    public RepairService repairs() {
        return retrofit(new TypeToken<PageImpl<Repair>>() {}.getType(),
                        new TypeToken<List<Repair>>() {}.getType())
                .create(RepairService.class);
    }

    @Override
    public StagingService staging() {
        return retrofit(new TypeToken<PageImpl<StagingStorageModel>>() {}.getType(),
                        new TypeToken<List<StagingStorageModel>>() {}.getType())
                .create(StagingService.class);
    }

    @Override
    public StorageService storage() {
        return retrofit(new TypeToken<PageImpl<StorageRegion>>() {}.getType(),
                        new TypeToken<List<StorageRegion>>() {}.getType())
                .create(StorageService.class);
    }

    @Override
    public ReplicationService replications() {
        return retrofit(new TypeToken<PageImpl<Replication>>() {}.getType(),
                        new TypeToken<List<Replication>>() {}.getType())
                .create(ReplicationService.class);
    }

    /**
     * Build the retrofit base class for a service
     *
     * @param page the type to capture for PageImpl[T]
     * @param list type type to capture for List[T]
     * @return the retrofit builder...class... thing
     */
    private Retrofit retrofit(Type page, Type list) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(page, new PageDeserializer(list))
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

 }
