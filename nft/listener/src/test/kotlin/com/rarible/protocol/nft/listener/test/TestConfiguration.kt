package com.rarible.protocol.nft.listener.test

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainClient
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.ethereum.client.cache.CacheableMonoEthereum
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.test.TestKafkaHandler
import io.daonomic.rpc.mono.WebClientTransport
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
            delegate = MonoEthereum(transport),
            expireAfter = Duration.ofMinutes(1),
            cacheMaxSize = 100,
            enableCacheByNumber = false,
            blockByNumberCacheExpireAfter = Duration.ofMinutes(1),
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
    fun testCollectionWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: TestKafkaHandler<NftCollectionEventDto>
    ): RaribleKafkaConsumerWorker<NftCollectionEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = nftIndexerProperties.kafkaReplicaSet,
            group = "test-group-collection-event",
            topic = NftCollectionEventTopicProvider.getTopic(
                application.name,
                nftIndexerProperties.blockchain.value
            ),
            valueClass = NftCollectionEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    fun testActivityHandler(): TestKafkaHandler<EthActivityEventDto> = TestKafkaHandler()

    @Bean
    fun testActivityWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: TestKafkaHandler<EthActivityEventDto>
    ): RaribleKafkaConsumerWorker<EthActivityEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = nftIndexerProperties.kafkaReplicaSet,
            topic = ActivityTopicProvider.getActivityTopic(
                application.name,
                nftIndexerProperties.blockchain.value
            ),
            group = "test-group-order-activity",
            valueClass = EthActivityEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    @Primary
    fun nftIndexerEventsConsumerFactory(env: ApplicationEnvironmentInfo) = NftIndexerEventsConsumerFactory(
        brokerReplicaSet = nftIndexerProperties.kafkaReplicaSet,
        host = "localhost",
        environment = env.name
    )
}
