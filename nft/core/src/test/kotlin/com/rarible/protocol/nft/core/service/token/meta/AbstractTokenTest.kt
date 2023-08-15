package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.meta.resource.resolver.GatewayProvider
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import io.mockk.InternalPlatformDsl.toStr
import io.netty.resolver.DefaultAddressResolverGroup
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.net.URI

abstract class AbstractTokenTest : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var urlService: UrlService

    @Autowired
    protected lateinit var tokenUriResolver: BlockchainTokenUriResolver

    @Autowired
    private lateinit var internalGatewaysProvider: GatewayProvider

    protected fun mockOpenSeaResponse(resourceName: String): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Assertions.assertThat(request.url().toStr())
                    .startsWith("https://api.opensea.io/api/v1/asset_contract/0x")
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body(resourceName.asResource())
                        .build()
                )
            }.build()
    }

    protected fun mockSuccessIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Assertions.assertThat(request.url())
                    // URL taken from application-integration.properties
                    .isEqualTo(URI("https://ipfs.io/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"))
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body("ipfs.json".asResource())
                        .build()
                )
            }.build()
    }

    protected fun mockNotFoundIpfsResponse(): WebClient {
        val httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeFunction { request ->
                Assertions.assertThat(request.url()).isEqualTo(
                    URI("${internalGatewaysProvider.getGateway()}/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T")
                )
                Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
            }.build()
    }

    protected fun mockBrokenJsonIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "plain/text")
                        .body("Hello world!")
                        .build()
                )
            }.build()
    }

    private fun String.asResource() = AbstractTokenTest.javaClass.getResource("/meta/response/$this").readText()

    companion object {
        const val OPENSEA_URL = "https://api.opensea.io/api/v1"
    }
}
