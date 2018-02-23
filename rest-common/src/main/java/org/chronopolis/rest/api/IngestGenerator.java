package org.chronopolis.rest.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.DepositorModel;
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<Bag>>() {},
                new TypeToken<List<Bag>>() {}))
                .create(BagService.class);
    }

    @Override
    public TokenService tokens() {
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<AceTokenModel>>() {},
                new TypeToken<List<AceTokenModel>>() {}))
                .create(TokenService.class);
    }

    @Override
    public RepairService repairs() {
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<Repair>>() {},
                new TypeToken<List<Repair>>() {}))
                .create(RepairService.class);
    }

    @Override
    public StagingService staging() {
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<StagingStorageModel>>() {},
                new TypeToken<List<StagingStorageModel>>() {}))
                .create(StagingService.class);
    }

    @Override
    public DepositorAPI depositorAPI() {
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<DepositorModel>>() {},
                new TypeToken<List<DepositorModel>>() {},
                new TypeToken<PageImpl<Bag>>() {},
                new TypeToken<List<Bag>>() {}))
                .create(DepositorAPI.class);
    }

    @Override
    public StorageService storage() {
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<StorageRegion>>() {},
                new TypeToken<List<StorageRegion>>() {}))
                .create(StorageService.class);
    }

    @Override
    public ReplicationService replications() {
        return retrofit(ImmutableMap.of(
                new TypeToken<PageImpl<Replication>>() {},
                new TypeToken<List<Replication>>() {}))
                .create(ReplicationService.class);
    }

    /**
     * Build the retrofit base class for a service
     *
     * @param types Map holding the relationship for a Page -> List type conversion
     * @return the retrofit builder...class... thing
     */
    private Retrofit retrofit(Map<TypeToken<? extends PageImpl<?>>,
                                  TypeToken<? extends List<?>>> types) {
        GsonBuilder gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer());

        types.forEach((page, list) ->
                gson.registerTypeAdapter(page.getType(), new PageDeserializer(list.getType())));

        final String username = properties.getUsername();
        final String password = properties.getPassword();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(username, password))
                .build();

        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson.create()))
                .baseUrl(properties.getEndpoint())
                .client(client)
                .build();
    }

}
