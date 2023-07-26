package com.rarible.protocol.nft.listener.test

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainClient
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.cache.CacheableMonoEthereum
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.core.TestKafkaHandler
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.consumer.KafkaEntityEventConsumer
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionPoller
import java.time.Duration

@TestConfiguration
@ComponentScan(basePackageClasses = [IntegrationTest::class])
class TestConfiguration {

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Bean
    fun testEthereum(@Value("\${parityUrls}") url: String): MonoEthereum {
        val transport = WebClientTransport(url, MonoEthereum.mapper(), 10000, 10000)
        return CacheableMonoEthereum(
            transport = transport,
            expireAfter = Duration.ofMinutes(1),
            cacheMaxSize = 100
        )
    }

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["common.feature-flags.scanner-version"], havingValue = "V2")
    fun testEthereumBlockchainClient(
        blockchainClient: EthereumBlockchainClient
    ): EthereumBlockchainClient {
        return TestEthereumBlockchainClient(blockchainClient)
    }

    @Bean
    fun testCollectionHandler(): TestKafkaHandler<NftCollectionEventDto> = TestKafkaHandler()

    @Bean
    fun testCollectionWorker(handler: TestKafkaHandler<NftCollectionEventDto>): ConsumerWorker<NftCollectionEventDto> {
        val consumer = RaribleKafkaConsumer(
            clientId = "test-consumer-collection-event",
            consumerGroup = "test-group-collection-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftCollectionEventDto::class.java,
            defaultTopic = NftCollectionEventTopicProvider.getTopic(
                application.name,
                nftIndexerProperties.blockchain.value
            ),
            bootstrapServers = nftIndexerProperties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        return ConsumerWorker(consumer, handler, "test-kafka-collection-worker")
    }

    @Bean
    fun testActivityHandler(): TestKafkaHandler<EthActivityEventDto> = TestKafkaHandler()

    @Bean
    fun testActivityWorker(handler: TestKafkaHandler<EthActivityEventDto>): ConsumerWorker<EthActivityEventDto> {
        val consumer = RaribleKafkaConsumer(
            clientId = "test-consumer-order-activity",
            consumerGroup = "test-group-order-activity",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = EthActivityEventDto::class.java,
            defaultTopic = ActivityTopicProvider.getActivityTopic(
                application.name,
                nftIndexerProperties.blockchain.value
            ),
            bootstrapServers = nftIndexerProperties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        return ConsumerWorker(consumer, handler, "test-kafka-activity-worker")
    }

    @Bean
    @Primary
    fun nftIndexerEventsConsumerFactory(env: ApplicationEnvironmentInfo) = NftIndexerEventsConsumerFactory(
        brokerReplicaSet = nftIndexerProperties.kafkaReplicaSet,
        host = "localhost",
        environment = env.name
    )

    @Bean
    @Primary
    fun testEntityEventConsumer(): KafkaEntityEventConsumer = mockk()
}
