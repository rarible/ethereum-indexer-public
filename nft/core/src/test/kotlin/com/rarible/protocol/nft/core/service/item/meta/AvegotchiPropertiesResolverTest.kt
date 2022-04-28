package com.rarible.protocol.nft.core.service.item.meta

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.AvegotchiPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaPropertiesResolver
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address


@ItemMetaTest
@EnabledIfSystemProperty(named = "RARIBLE_TESTS_OPENSEA_PROXY_URL", matches = ".+")
class AvegotchiPropertiesResolverTest : BasePropertiesResolverTest() {

    private val lazyNftItemHistoryRepository = mockk<LazyNftItemHistoryRepository>()

    private val ipfsServiceMock = mockk<IpfsService>()

    private val openseaPropertiesResolverMock = mockk<OpenSeaPropertiesResolver>()

    private var wireMockServer: WireMockServer? = null
    private var webClient: WebClient? = null
    private var baseUrl: String? = null

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer!!.start()
        baseUrl = wireMockServer!!.baseUrl()
        webClient = WebClient.builder().build()

        wireMockServer!!.stubFor(
            get(urlEqualTo("/metadata/aavegotchis/21089"))
                .willReturn(
                    aResponse()
                        .withStatus(303)
                        .withBody("")
                )
        )
    }


    private val avegotchiPropertiesResolver: AvegotchiPropertiesResolver = AvegotchiPropertiesResolver(
        sender = createSender(),
        tokenRepository = tokenRepository,
        ipfsService = ipfsServiceMock,
        requestTimeout = 20000,
        externalHttpClient = OpenSeaPropertiesResolverTest.createExternalHttpClient(),
        openSeaPropertiesResolver = openseaPropertiesResolverMock
    )

    @BeforeEach
    @Suppress("ReactiveStreamsUnusedPublisher")
    private fun before() {
        clearMocks(lazyNftItemHistoryRepository)
        every { lazyNftItemHistoryRepository.findLazyMintById(any()) } returns Mono.empty()
        every { lazyNftItemHistoryRepository.find(any(), any(), any()) } returns Flux.empty()
    }

    /**
     * Aavegotchi resolver should trigger Opensea on 3XX redirrect response from Avegotchi server
     */
    @Test
    fun aavegotchi_3xx() = runBlocking<Unit> {
        every {
            ipfsServiceMock.resolveInnerHttpUrl("https://aavegotchi.com/metadata/aavegotchis/21089")
        } returns "$baseUrl/metadata/aavegotchis/21089"
        coEvery { openseaPropertiesResolverMock.resolve(any()) } returns ItemPropertiesWrapper(null, true)
        val address = Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70")
        mockTokenStandard(address, TokenStandard.ERC721)
        avegotchiPropertiesResolver.resolve(ItemId(address, EthUInt256.of(21089))).itemProperties
        coVerify(exactly = 1) { openseaPropertiesResolverMock.resolve(any()) }
    }
}
