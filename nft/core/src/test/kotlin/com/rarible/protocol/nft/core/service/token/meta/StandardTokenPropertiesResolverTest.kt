package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.meta.resource.http.DefaultHttpClient
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.OpenseaHttpClient
import com.rarible.core.meta.resource.http.ProxyHttpClient
import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.builder.ProxyWebClientBuilder
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.BasePropertiesResolverTest
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import com.rarible.protocol.nft.core.test.IntegrationTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.web3jold.utils.Numeric
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender

@IntegrationTest
class StandardTokenPropertiesResolverTest : AbstractTokenTest() {
    private lateinit var userSender: MonoSigningTransactionSender
    private lateinit var erc721: ERC721Rarible

    @BeforeEach
    fun before() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        userSender = newSender(privateKey).second
        erc721 = ERC721Rarible.deployAndWait(userSender, poller).awaitFirst()
        erc721.__ERC721Rarible_init(
            "Feudalz",
            "FEUDALZ",
            "baseURI",
            "ipfs://QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"
        ).execute().verifySuccess()
        val tokenToSave = createToken().copy(
            id = erc721.address(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(tokenToSave).awaitSingle()
    }

    @Test
    fun `should parse from ipfs`() = runBlocking<Unit> {
        val resolver = mockStandardTokenPropertiesResolver(mockSuccessIpfsResponse())
        val props = resolver.resolve(erc721.address())
        assertThat(props).isEqualTo(TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            externalUri = "https://feudalz.io",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            tokenUri = "ipfs://QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T",
            sellerFeeBasisPoints = 250,
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"
            )
        ))
    }

    @Test
    fun `should return null if not found`() = runBlocking<Unit> {
        val resolver = mockStandardTokenPropertiesResolver(mockNotFoundIpfsResponse())
        val props = resolver.resolve(erc721.address())
        assertThat(props).isNull()
    }

    @Test
    fun `should return null if broken json`() = runBlocking<Unit> {
        val resolver = mockStandardTokenPropertiesResolver(mockBrokenJsonIpfsResponse())
        val props = resolver.resolve(erc721.address())
        assertThat(props).isNull()
    }

    private fun mockStandardTokenPropertiesResolver(webClient: WebClient): StandardTokenPropertiesResolver =
        StandardTokenPropertiesResolver(
            urlService = urlService,
            externalHttpClient = mockExternalHttpClient(webClient),
            tokenUriResolver = tokenUriResolver
        )

    private fun mockExternalHttpClient(webClient: WebClient): ExternalHttpClient {
        val proxyWebClientBuilder = ProxyWebClientBuilder(
            readTimeout = 10000,
            connectTimeout = 3000,
            proxyUrl = "",
            followRedirect = true
        )

        val defaultWebClientBuilder: DefaultWebClientBuilder = mockk()
        coEvery { defaultWebClientBuilder.build() } returns webClient

        val defaultHttpClient = DefaultHttpClient(
            builder = defaultWebClientBuilder,
            requestTimeout = BasePropertiesResolverTest.REQUEST_TIMEOUT
        )

        val proxyHttpClient = ProxyHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = BasePropertiesResolverTest.REQUEST_TIMEOUT
        )

        val openseaHttpClient = OpenseaHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = BasePropertiesResolverTest.REQUEST_TIMEOUT,
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
