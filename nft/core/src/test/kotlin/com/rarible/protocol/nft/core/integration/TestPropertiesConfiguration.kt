package com.rarible.protocol.nft.core.integration

import com.rarible.blockchain.scanner.reconciliation.DefaultReconciliationFormProvider
import com.rarible.blockchain.scanner.reconciliation.ReconciliationFromProvider
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.cache.CacheService
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.InternalItemHandler
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolverProvider
import com.rarible.protocol.nft.core.service.token.meta.InternalCollectionHandler
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import io.daonomic.rpc.mono.WebClientTransport
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import scalether.core.EthPubSub
import scalether.core.MonoEthereum
import scalether.core.PubSubTransport
import scalether.domain.Address
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.ReadOnlyMonoTransactionSender
import scalether.transport.WebSocketPubSubTransport

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun skipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(hashSetOf())
    }

    @Bean
    fun reconciliationFromProvider(): ReconciliationFromProvider {
        return DefaultReconciliationFormProvider()
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
    fun itemMetaExtenderWorker() {

    }

    @Bean
    @Primary
    @Qualifier("mockItemPropertiesResolver")
    fun mockItemPropertiesResolver(): ItemPropertiesResolver = mockk {
        every { name } returns "MockResolver"
        every { canBeCached } returns true
    }

    @Bean
    @Primary
    fun mockItemPropertiesResolverProvider(
        @Qualifier("mockItemPropertiesResolver") mockItemPropertiesResolver: ItemPropertiesResolver
    ): ItemPropertiesResolverProvider = mockk {
        every { orderedResolvers } returns listOf(mockItemPropertiesResolver)
    }

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
        return object : TokenPropertiesService(60000, mockk(), listOf(mockStandardTokenPropertiesResolver)) {
            override suspend fun resolve(id: Address): TokenProperties? {
                return super.doResolve(id)
            }
        }
    }

    /**
     * This bean is needed to make possible publishing of item with extended meta.
     * In production this bean is defined in the 'nft-indexer-listener' module.
     */
    @Bean
    fun itemMetaExtenderWorker(
        applicationEnvironmentInfo: ApplicationEnvironmentInfo,
        internalItemHandler: InternalItemHandler,
        nftIndexerProperties: NftIndexerProperties,
        meterRegistry: MeterRegistry
    ): ConsumerWorker<NftItemEventDto> {
        return ConsumerWorker(
            consumer = InternalItemHandler.createInternalItemConsumer(
                applicationEnvironmentInfo,
                nftIndexerProperties.blockchain,
                nftIndexerProperties.kafkaReplicaSet
            ),
            properties = nftIndexerProperties.daemonWorkerProperties,
            eventHandler = internalItemHandler,
            meterRegistry = meterRegistry,
            workerName = "nftItemMetaExtender"
        )
    }

    /**
     * This bean is needed to make possible publishing of collection with extended meta.
     * In production this bean is defined in the 'nft-indexer-listener' module.
     */
    @Bean
    fun collectionMetaExtenderWorker(
        applicationEnvironmentInfo: ApplicationEnvironmentInfo,
        internalCollectionHandler: InternalCollectionHandler,
        nftIndexerProperties: NftIndexerProperties,
        meterRegistry: MeterRegistry
    ): ConsumerWorker<NftCollectionEventDto> {
        return ConsumerWorker(
            consumer = InternalCollectionHandler.createInternalCollectionConsumer(
                applicationEnvironmentInfo,
                nftIndexerProperties.blockchain,
                nftIndexerProperties.kafkaReplicaSet
            ),
            properties = nftIndexerProperties.daemonWorkerProperties,
            eventHandler = internalCollectionHandler,
            meterRegistry = meterRegistry,
            workerName = "nftCollectionMetaExtender"
        )
    }

    @Bean
    fun itemMetaExtenderWorkerStarter(itemMetaExtenderWorker: ConsumerWorker<NftItemEventDto>): CommandLineRunner =
        CommandLineRunner { itemMetaExtenderWorker.start() }

    @Bean
    fun collectionMetaExtenderWorkerStarter(collectionMetaExtenderWorker: ConsumerWorker<NftCollectionEventDto>): CommandLineRunner =
        CommandLineRunner { collectionMetaExtenderWorker.start() }
}
