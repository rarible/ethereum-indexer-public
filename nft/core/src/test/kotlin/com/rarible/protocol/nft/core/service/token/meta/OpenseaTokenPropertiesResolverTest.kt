package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.meta.resource.http.DefaultHttpClient
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.OpenseaHttpClient
import com.rarible.core.meta.resource.http.ProxyHttpClient
import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.builder.ProxyWebClientBuilder
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.BasePropertiesResolverTest.Companion.REQUEST_TIMEOUT
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.token.meta.descriptors.OpenseaTokenPropertiesResolver
import com.rarible.protocol.nft.core.test.IntegrationTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import scalether.domain.Address
import scalether.domain.AddressFactory

@IntegrationTest
class OpenseaTokenPropertiesResolverTest : AbstractTokenTest() {

    private lateinit var resolver: OpenseaTokenPropertiesResolver
    private lateinit var token: Token

    @BeforeEach
    fun before() = runBlocking<Unit> {
        token = createToken().copy(
            id = AddressFactory.create(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(token).awaitSingle()
    }

    @Test
    fun `should parse from json`() = runBlocking<Unit> {
        resolver = mockOpenseaTokenPropertiesResolver(
            mockOpenSeaResponse("opensea.json")
        )
        val props = resolver.resolve(token.id)
        assertThat(props).isEqualTo(
            TokenProperties(
                name = "Feudalz",
                description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
                externalUri = "https://feudalz.io",
                feeRecipient = Address.apply("0xc00f4b8022e4dc7f086d703328247cb6adf26858"),
                sellerFeeBasisPoints = 250,
                content = ContentBuilder.getTokenMetaContent(
                    imageOriginal = "https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120"
                )
            )
        )
    }

    @Test
    fun `should parse with unidentified contract name`() = runBlocking<Unit> {
        resolver = mockOpenseaTokenPropertiesResolver(
            mockOpenSeaResponse("opensea_unidentified_contract.json")
        )
        val props = resolver.resolve(token.id)
        assertThat(props).isEqualTo(
            TokenProperties(
                name = "My contract",
                description = null,
                externalUri = null,
                feeRecipient = null,
                sellerFeeBasisPoints = null,
                content = ContentBuilder.getTokenMetaContent(
                    imageOriginal = null
                )
            )
        )
    }

    fun mockOpenseaTokenPropertiesResolver(webClient: WebClient): OpenseaTokenPropertiesResolver {
        val externalHttpClient = mockExternalHttpClient(webClient)
        return OpenseaTokenPropertiesResolver(
            externalHttpClient = externalHttpClient,
            properties = mockk<NftIndexerProperties> {
                every { opensea } returns NftIndexerProperties.OpenseaProperties().copy(url = OPENSEA_URL)
            }
        )
    }

    private fun mockExternalHttpClient(webClient: WebClient): ExternalHttpClient {
        val proxyWebClientBuilder: ProxyWebClientBuilder = mockk()
        coEvery { proxyWebClientBuilder.build() } returns webClient

        val defaultWebClientBuilder = DefaultWebClientBuilder(followRedirect = true)

        val defaultHttpClient = DefaultHttpClient(
            builder = defaultWebClientBuilder,
            requestTimeout = REQUEST_TIMEOUT
        )

        val proxyHttpClient = ProxyHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = REQUEST_TIMEOUT
        )

        val openseaHttpClient = OpenseaHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = REQUEST_TIMEOUT,
            openseaUrl = OPENSEA_URL,
            openseaApiKey = "",
            proxyUrl = "",
        )

        return ExternalHttpClient(
            defaultClient = defaultHttpClient,
            proxyClient = proxyHttpClient,
            customClients = listOf(openseaHttpClient)
        )
    }
}
