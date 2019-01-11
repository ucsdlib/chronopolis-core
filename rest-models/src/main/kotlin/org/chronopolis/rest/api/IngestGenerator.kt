package org.chronopolis.rest.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.chronopolis.rest.models.FulfillmentStrategy
import org.chronopolis.rest.models.enums.FixityAlgorithm
import org.chronopolis.rest.models.serializers.FixityAlgorithmDeserializer
import org.chronopolis.rest.models.serializers.FixityAlgorithmSerializer
import org.chronopolis.rest.models.serializers.FulfillmentStrategyDeserializer
import org.chronopolis.rest.models.serializers.FulfillmentStrategySerializer
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.time.ZonedDateTime

class IngestGenerator(val properties: IngestApiProperties) : ServiceGenerator {
    override fun bags(): BagService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(BagService::class.java)
    }

    override fun files(): FileService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(FileService::class.java)
    }

    override fun tokens(): TokenService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(TokenService::class.java)
    }

    override fun repairs(): RepairService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(RepairService::class.java)
    }

    override fun staging(): StagingService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(StagingService::class.java)
    }

    override fun depositors(): DepositorService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(DepositorService::class.java)
    }

    override fun storage(): StorageService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(StorageService::class.java)
    }

    override fun replications(): ReplicationService {
        return Retrofit.Builder()
                .client(client())
                .baseUrl(properties.endpoint)
                .addConverterFactory(converterFactory())
                .build().create(ReplicationService::class.java)
    }

    private fun converterFactory(): JacksonConverterFactory {
        val mapper = ObjectMapper()

        val module = SimpleModule()

        module.addSerializer(ZonedDateTime::class.java, ZonedDateTimeSerializer())
        module.addDeserializer(ZonedDateTime::class.java, ZonedDateTimeDeserializer())

        module.addSerializer(FixityAlgorithm::class.java, FixityAlgorithmSerializer())
        module.addDeserializer(FixityAlgorithm::class.java, FixityAlgorithmDeserializer())

        module.addSerializer(FulfillmentStrategy::class.java, FulfillmentStrategySerializer())
        module.addDeserializer(FulfillmentStrategy::class.java, FulfillmentStrategyDeserializer())

        mapper.registerModule(module)
        mapper.registerModule(KotlinModule())

        return JacksonConverterFactory.create(mapper)
    }

    private fun client(): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor(OkBasicInterceptor(properties.username, properties.password))
                .build()
    }
}
