package com.rarible.protocol.gateway

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.gateway.api.client.GatewayApiClientFactory
import com.rarible.protocol.gateway.api.client.NftCollectionControllerApi
import com.rarible.protocol.gateway.api.client.NftItemControllerApi
import com.rarible.protocol.gateway.api.client.NftLazyMintControllerApi
import com.rarible.protocol.gateway.api.client.NftOwnershipControllerApi
import com.rarible.protocol.gateway.client.FixedGatewayApiServiceUriProvider

import org.springframework.boot.web.server.LocalServerPort
import java.net.URI
import javax.annotation.PostConstruct

abstract class AbstractIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    protected lateinit var nftCollectionApi: NftCollectionControllerApi
    protected lateinit var nftLazyMint: NftLazyMintControllerApi
    protected lateinit var nftItemApi: NftItemControllerApi
    protected lateinit var nftOwnershipApi: NftOwnershipControllerApi

    protected val mapper = kotlin.run {
        val objectMapper = ObjectMapper()
        objectMapper.registerKotlinModule()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper
    }

    @PostConstruct
    fun setUp() {
        val urlProvider = FixedGatewayApiServiceUriProvider(URI.create("http://127.0.0.1:$port"))
        val clientsFactory = GatewayApiClientFactory(urlProvider, NoopWebClientCustomizer())

        nftCollectionApi = clientsFactory.createNftCollectionApiClient(Blockchain.ETHEREUM.value)
        nftLazyMint = clientsFactory.createNftMintApiClient(Blockchain.ETHEREUM.value)
        nftItemApi = clientsFactory.createNftItemApiClient(Blockchain.ETHEREUM.value)
        nftOwnershipApi = clientsFactory.createNftOwnershipApiClient(Blockchain.ETHEREUM.value)
    }
}
