package org.chronopolis.ingest.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.DepositorContact;
import org.chronopolis.rest.entities.projections.CompleteBag;
import org.chronopolis.rest.entities.projections.PartialBag;
import org.chronopolis.rest.entities.projections.ReplicationView;
import org.chronopolis.rest.entities.repair.Repair;
import org.chronopolis.rest.entities.serializers.AceTokenSerializer;
import org.chronopolis.rest.entities.serializers.BagSerializer;
import org.chronopolis.rest.entities.serializers.CompleteBagSerializer;
import org.chronopolis.rest.entities.serializers.DataFileSerializer;
import org.chronopolis.rest.entities.serializers.DepositorContactSerializer;
import org.chronopolis.rest.entities.serializers.DepositorSerializer;
import org.chronopolis.rest.entities.serializers.PartialBagSerializer;
import org.chronopolis.rest.entities.serializers.RepairSerializer;
import org.chronopolis.rest.entities.serializers.ReplicationSerializer;
import org.chronopolis.rest.entities.serializers.ReplicationViewSerializer;
import org.chronopolis.rest.entities.serializers.StagingStorageSerializer;
import org.chronopolis.rest.entities.serializers.StorageRegionSerializer;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.FulfillmentStrategy;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.chronopolis.rest.models.serializers.FixityAlgorithmDeserializer;
import org.chronopolis.rest.models.serializers.FixityAlgorithmSerializer;
import org.chronopolis.rest.models.serializers.FulfillmentStrategyDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author shake
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("/public", "classpath:/static/")
                .setCachePeriod(31556926);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder()
                .indentOutput(true);
        builder.indentOutput(true);
        builder.modulesToInstall(new KotlinModule());
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.serializerByType(Bag.class, new BagSerializer());
        builder.serializerByType(PartialBag.class, new PartialBagSerializer());
        builder.serializerByType(CompleteBag.class, new CompleteBagSerializer());
        builder.serializerByType(ReplicationView.class, new ReplicationViewSerializer());
        builder.serializerByType(Repair.class, new RepairSerializer());
        builder.serializerByType(BagFile.class, new DataFileSerializer());
        builder.serializerByType(AceToken.class, new AceTokenSerializer());
        builder.serializerByType(TokenStore.class, new DataFileSerializer());
        builder.serializerByType(Depositor.class, new DepositorSerializer());
        builder.serializerByType(Replication.class, new ReplicationSerializer());
        builder.serializerByType(StorageRegion.class, new StorageRegionSerializer());
        builder.serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer());
        builder.serializerByType(StagingStorage.class, new StagingStorageSerializer());
        builder.serializerByType(FixityAlgorithm.class, new FixityAlgorithmSerializer());
        builder.serializerByType(DepositorContact.class, new DepositorContactSerializer());
        builder.deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        builder.deserializerByType(FixityAlgorithm.class, new FixityAlgorithmDeserializer());
        builder.deserializerByType(FulfillmentStrategy.class,
                new FulfillmentStrategyDeserializer());

        converters.add(new MappingJackson2HttpMessageConverter(builder.build()));
    }
}
