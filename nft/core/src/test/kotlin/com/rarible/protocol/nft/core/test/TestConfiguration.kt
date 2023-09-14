package com.rarible.protocol.nft.core.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaResolver
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import io.daonomic.rpc.mono.WebClientTransport
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import scalether.core.EthPubSub
import scalether.core.MonoEthereum
import scalether.core.PubSubTransport
import scalether.domain.Address
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.ReadOnlyMonoTransactionSender
import scalether.transport.WebSocketPubSubTransport

@TestConfiguration
@ComponentScan(basePackageClasses = [IntegrationTest::class])
class TestConfiguration {

    @Autowired
    protected lateinit var properties: NftIndexerProperties

    @Bean
    fun skipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(hashSetOf())
    }

    @Bean
    fun testEthereum(@Value("\${parityUrls}") url: String): MonoEthereum {
        return MonoEthereum(WebClientTransport(url, MonoEthereum.mapper(), 10000, 10000))
    }

    @Bean
    fun testSender(ethereum: MonoEthereum) = ReadOnlyMonoTransactionSender(ethereum, Address.ONE())

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }

    @Bean
    fun pubSubTransport(@Value("\${parityUrls}") url: String): WebSocketPubSubTransport {
        return WebSocketPubSubTransport(url, Int.MAX_VALUE)
    }

    @Bean
    fun ethPubSub(transport: PubSubTransport): EthPubSub {
        return EthPubSub(transport)
    }

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("localhost", "e2e")
    }

    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

    @Bean
    @Primary
    @Qualifier("mockItemMetaResolver")
    fun mockItemMetaResolver(): ItemMetaResolver = mockk()

    @Bean
    @Primary
    @Qualifier("mockExternalHttpClient")
    fun mockExternalHttpClient(): ExternalHttpClient = mockk()

    @Bean
    @Primary
    @Qualifier("mockStandardTokenPropertiesResolver")
    fun mockStandardTokenPropertiesResolver(): StandardTokenPropertiesResolver = mockk {
        every { order } returns Int.MIN_VALUE
    }

    @Bean
    @Primary
    fun testTokenPropertiesService(
        @Qualifier("mockStandardTokenPropertiesResolver") mockStandardTokenPropertiesResolver: StandardTokenPropertiesResolver
    ): TokenPropertiesService {
        return object : TokenPropertiesService(
            listOf(mockStandardTokenPropertiesResolver),
            mockk(),
        ) {
            override suspend fun resolve(id: Address): TokenProperties? {
                return super.resolve(id)
            }
        }
    }

    @Bean
    fun testOwnershipEventHandler() = TestKafkaHandler<NftOwnershipEventDto>()

    @Bean
    fun testOwnershipEventConsumer(
        factory: RaribleKafkaConsumerFactory,
        application: ApplicationEnvironmentInfo,
        handler: TestKafkaHandler<NftOwnershipEventDto>
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.kafkaReplicaSet,
            group = "test-group-ownership-event",
            topic = NftOwnershipEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            valueClass = NftOwnershipEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    fun testItemEventHandler() = TestKafkaHandler<NftItemEventDto>()

    @Bean
    fun testItemEventConsumer(
        factory: RaribleKafkaConsumerFactory,
        application: ApplicationEnvironmentInfo,
        handler: TestKafkaHandler<NftItemEventDto>
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.kafkaReplicaSet,
            group = "test-group-item-event",
            topic = NftItemEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            valueClass = NftItemEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    fun testCollectionEventHandler() = TestKafkaHandler<NftCollectionEventDto>()

    @Bean
    fun testCollectionEventConsumer(
        factory: RaribleKafkaConsumerFactory,
        application: ApplicationEnvironmentInfo,
        handler: TestKafkaHandler<NftCollectionEventDto>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.kafkaReplicaSet,
            group = "test-group-collection-event",
            topic = NftCollectionEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            valueClass = NftCollectionEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    fun testItemMetaEventHandler() = TestKafkaHandler<NftItemMetaEventDto>()

    @Bean
    fun testItemMetaEventConsumer(
        factory: RaribleKafkaConsumerFactory,
        application: ApplicationEnvironmentInfo,
        handler: TestKafkaHandler<NftItemMetaEventDto>
    ): RaribleKafkaConsumerWorker<NftItemMetaEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.kafkaReplicaSet,
            group = "test-group-item-meta-event",
            topic = NftItemEventTopicProvider.getItemMetaTopic(application.name, properties.blockchain.value),
            valueClass = NftItemMetaEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        )
        return factory.createWorker(settings, handler)
    }
}
