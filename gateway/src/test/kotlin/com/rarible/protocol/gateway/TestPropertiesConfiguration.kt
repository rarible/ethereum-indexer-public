package com.rarible.protocol.gateway

import com.rarible.protocol.erc20.api.client.Erc20IndexerApiServiceUriProvider
import com.rarible.protocol.erc20.api.client.FixedErc20IndexerApiServiceUriProvider
import com.rarible.protocol.nft.api.client.*
import com.rarible.protocol.order.api.client.FixedOrderIndexerApiServiceUriProvider
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiServiceUriProvider
import com.rarible.protocol.unlockable.api.client.SwarmUnlockableApiServiceUriProvider
import com.rarible.protocol.unlockable.api.client.UnlockableApiServiceUriProvider
import io.mockk.mockk
import org.mockserver.client.MockServerClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean()
    @Qualifier("mockNftServerClient")
    fun mockNftServerClient(): MockServerClient {
        return MockServerClient(nftMockServer.host, nftMockServer.serverPort)
    }

    @Bean()
    @Qualifier("mockNftOrderServerClient")
    fun mockNftOrderServerClient(): MockServerClient {
        return MockServerClient(nftOrderMockServer.host, nftOrderMockServer.serverPort)
    }

    @Bean
    fun nftIndexerApiServiceUriProvider(): NftIndexerApiServiceUriProvider {
        return FixedNftIndexerApiServiceUriProvider(URI.create(nftMockServer.endpoint))
    }

    @Bean
    fun erc20IndexerApiServiceUriProvider(): Erc20IndexerApiServiceUriProvider {
        return FixedErc20IndexerApiServiceUriProvider(URI.create(erc20MockServer.endpoint))
    }

    @Bean
    fun orderIndexerApiServiceUriProvider(): OrderIndexerApiServiceUriProvider {
        return FixedOrderIndexerApiServiceUriProvider(URI.create(orderMockServer.endpoint))
    }

    @Bean
    fun unlockableApiServiceUriProvider(): UnlockableApiServiceUriProvider {
        return SwarmUnlockableApiServiceUriProvider("e2e")
    }

    @Bean
    @Primary
    fun testNftActivityControllerApi(): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    fun testOrderActivityControllerApi(): OrderActivityControllerApi = mockk()

    @Bean
    fun testRestTemplate(): RestTemplate {
        return RestTemplate()
    }

    companion object {
        private val nftMockServer = MockServerContainer(DockerImageName.parse("mockserver/mockserver"))
        private val erc20MockServer = MockServerContainer(DockerImageName.parse("mockserver/mockserver"))
        private val orderMockServer = MockServerContainer(DockerImageName.parse("mockserver/mockserver"))
        private val nftOrderMockServer = MockServerContainer(DockerImageName.parse("mockserver/mockserver"))

        init {
            nftMockServer.start()
            erc20MockServer.start()
            orderMockServer.start()
            nftOrderMockServer.start()
        }
    }
}