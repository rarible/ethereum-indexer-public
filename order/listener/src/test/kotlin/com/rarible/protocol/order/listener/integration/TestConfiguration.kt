package com.rarible.protocol.order.listener.integration

import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordEventConsumer
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.ethereum.client.cache.CacheableMonoEthereum
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import com.rarible.protocol.erc20.api.client.BalanceControllerApi
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.service.approve.Erc20Service
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import com.rarible.protocol.order.listener.data.createErc20BalanceDto
import com.rarible.x2y2.client.X2Y2ApiClient
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.time.Duration

@TestConfiguration
class TestConfiguration {

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
    fun testReadOnlyMonoTransactionSender(ethereum: MonoEthereum): ReadOnlyMonoTransactionSender {
        return ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())
    }

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }

    @Bean
    @Primary
    fun mockedErc20BalanceApiClient(): BalanceControllerApi {
        return mockk {
            every { getErc20Balance(any(), any()) } returns Mono.just(createErc20BalanceDto())
        }
    }

    @Bean
    @Primary
    fun mockedX2Y2ApiClient(): X2Y2ApiClient {
        return mockk()
    }

    @Bean
    @Primary
    fun mockedCurrencyApi(): CurrencyControllerApi {
        return mockk {
            every { getCurrencyRate(any(), any(), any()) } returns CurrencyRateDto(
                "test",
                "usd",
                ETH_CURRENCY_RATE,
                nowMillis()
            ).toMono()
        }
    }

    @Bean
    @Primary
    fun mockAssetBalanceProvider(): AssetBalanceProvider = mockk {
    }

    @Bean
    @Primary
    fun mockERC1271SignService(): ERC1271SignService = mockk {
    }

    @Bean
    @Primary
    fun testEntityEventConsumer(): KafkaLogRecordEventConsumer = mockk()

    @Bean
    @Primary
    fun testErc20Service(): Erc20Service = mockk()

    @Bean
    fun testActivityEventHandler() = TestKafkaHandler<EthActivityEventDto>()

    @Bean
    fun testActivityConsumer(
        factory: RaribleKafkaConsumerFactory,
        application: ApplicationEnvironmentInfo,
        properties: OrderIndexerProperties,
        handler: TestKafkaHandler<EthActivityEventDto>
    ): RaribleKafkaConsumerWorker<EthActivityEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.kafkaReplicaSet,
            group = "test-group-order-activity",
            topic = ActivityTopicProvider.getActivityTopic(
                application.name,
                properties.blockchain.value
            ),
            valueClass = EthActivityEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    fun testAuctionEventHandler() = TestKafkaHandler<AuctionEventDto>()

    @Bean
    fun testAuctionConsumer(
        factory: RaribleKafkaConsumerFactory,
        application: ApplicationEnvironmentInfo,
        properties: OrderIndexerProperties,
        handler: TestKafkaHandler<AuctionEventDto>
    ): RaribleKafkaConsumerWorker<AuctionEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = properties.kafkaReplicaSet,
            group = "test-consumer-auction-activity",
            topic = OrderIndexerTopicProvider.getAuctionUpdateTopic(
                application.name,
                properties.blockchain.value
            ),
            valueClass = AuctionEventDto::class.java,
            concurrency = 1,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        return factory.createWorker(settings, handler)
    }

    companion object {

        val ETH_CURRENCY_RATE = 3000.toBigDecimal() // 3000$
    }
}
