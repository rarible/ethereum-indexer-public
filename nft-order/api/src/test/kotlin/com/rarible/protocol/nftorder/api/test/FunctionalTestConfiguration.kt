package com.rarible.protocol.nftorder.api.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.nft.api.client.*
import com.rarible.protocol.nftorder.api.client.*
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import java.net.URI

@Lazy
@TestConfiguration
class FunctionalTestConfiguration(
    val blockchain: Blockchain
) {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    // In case when we have dedicated mocks, it's better to define them as beans instead of using
    // @MockkBean - that allow Spring to reuse launched context for different tests and, as a result,
    // gives significant speedup for test's run

    @Bean
    @Qualifier("testLocalhostUri")
    fun testLocalhostUri(@LocalServerPort port: Int): URI {
        return URI("http://localhost:${port}")
    }

    @Bean
    fun testRestTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    @Primary
    fun testNftItemControllerApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testNftOwnershipControllerApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testNftActivityControllerApi(): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    fun testNftCollectionControllerApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    fun testNftLazyMintControllerApi(): NftLazyMintControllerApi = mockk()

    @Bean
    @Primary
    fun testOrderControllerApi(): OrderControllerApi = mockk()

    @Bean
    @Primary
    fun testOrderActivityControllerApi(): OrderActivityControllerApi = mockk()

    @Bean
    @Primary
    fun testLockControllerApi(): LockControllerApi = mockk()

    @Bean
    @Primary
    fun testNftOrderApiClientFactory(@Qualifier("testLocalhostUri") uri: URI): NftOrderApiClientFactory {
        return NftOrderApiClientFactory(
            FixedNftOrderApiServiceUriProvider(uri),
            NoopWebClientCustomizer()
        )
    }

    @Bean
    fun nftOrderItemControllerApi(nftOrderApiClientFactory: NftOrderApiClientFactory): NftOrderItemControllerApi {
        return nftOrderApiClientFactory.createNftOrderItemApiClient(blockchain.value)
    }

    @Bean
    fun nftOrderActivityControllerApi(nftOrderApiClientFactory: NftOrderApiClientFactory): NftOrderActivityControllerApi {
        return nftOrderApiClientFactory.createNftOrderActivityApiClient(blockchain.value)
    }

    @Bean
    fun nftOrderOwnershipControllerApi(nftOrderApiClientFactory: NftOrderApiClientFactory): NftOrderOwnershipControllerApi {
        return nftOrderApiClientFactory.createNftOrderOwnershipApiClient(blockchain.value)
    }

    @Bean
    fun nftOrderCollectionControllerApi(nftOrderApiClientFactory: NftOrderApiClientFactory): NftOrderCollectionControllerApi {
        return nftOrderApiClientFactory.createNftOrderCollectionControllerApi(blockchain.value)
    }

    @Bean
    fun nftOrderLazyMintControllerApi(nftOrderApiClientFactory: NftOrderApiClientFactory): NftOrderLazyMintControllerApi {
        return nftOrderApiClientFactory.createNftOrderLazyMintControllerApi(blockchain.value)
    }

    @Bean
    @Primary
    fun testItemEventProducer(): RaribleKafkaProducer<NftOrderItemEventDto> = mockk()

    @Bean
    @Primary
    fun testOwnershipEventProducer(): RaribleKafkaProducer<NftOrderOwnershipEventDto> = mockk()

}