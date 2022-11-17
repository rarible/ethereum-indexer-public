package com.rarible.protocol.nft.api.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.cache.CacheService
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.nft.core.TestKafkaHandler
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaResolver
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.OpenseaTokenPropertiesResolver
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.net.HttpURLConnection

@TestConfiguration
class TestConfiguration {

    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    private lateinit var properties: NftIndexerProperties

    @Bean
    @Primary
    @Qualifier("mockLazyNftValidator")
    fun mockLazyNftValidator(): LazyNftValidator = mockk()

    @Bean
    @Primary
    @Qualifier("mockItemMetaResolver")
    fun mockItemMetaResolver(): ItemMetaResolver = mockk()

    @Bean
    @Primary
    @Qualifier("mockStandardTokenPropertiesResolver")
    fun mockStandardTokenPropertiesResolver(): StandardTokenPropertiesResolver = mockk {
        every { order } returns -1
    }

    @Bean
    @Primary
    @Qualifier("mockOpenseaTokenPropertiesResolver")
    fun mockOpenseaTokenPropertiesResolver(): OpenseaTokenPropertiesResolver = mockk {
        every { order } returns 1
    }

    @Bean
    @Primary
    fun testTokenPropertiesService(
        cacheService: CacheService,
        @Qualifier("mockStandardTokenPropertiesResolver") standardPropertiesResolver: StandardTokenPropertiesResolver,
        @Qualifier("mockOpenseaTokenPropertiesResolver") openseaPropertiesResolver: OpenseaTokenPropertiesResolver
    ): TokenPropertiesService {
        return TokenPropertiesService(
            Long.MAX_VALUE,
            cacheService,
            listOf(standardPropertiesResolver, openseaPropertiesResolver),
            mockk(),
            mockk(),
            mockk(),
            FeatureFlags().copy(enableTokenMetaSelfRepair = false)
        )
    }

    @Bean
    @Primary
    @Qualifier("mockMediaMetaService")
    fun mockMediaMetaService(): MediaMetaService = mockk()

    @Bean
    fun testRestTemplate(): RestTemplate {
        return RestTemplate(object : SimpleClientHttpRequestFactory() {
            override fun prepareConnection(connection: HttpURLConnection, httpMethod: String?) {
                super.prepareConnection(connection, httpMethod)
                connection.instanceFollowRedirects = false
            }
        })
    }

    @Bean
    fun testItemHandler(): TestKafkaHandler<NftItemEventDto> = TestKafkaHandler()

    @Bean
    fun testActivityWorker(handler: TestKafkaHandler<NftItemEventDto>): ConsumerWorker<NftItemEventDto> {
        val consumer = RaribleKafkaConsumer(
            clientId = "test-consumer-item-event",
            consumerGroup = "test-group-item-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        return ConsumerWorker(consumer, handler, "test-kafka-activity-worker")
    }

    @Bean
    fun testLauncher(
        itemWorker: ConsumerWorker<NftItemEventDto>
    ): TestLauncher {
        return TestLauncher(itemWorker)
    }

}
