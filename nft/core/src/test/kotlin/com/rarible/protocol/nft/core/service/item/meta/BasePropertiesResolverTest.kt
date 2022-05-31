package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.ConstantGatewayProvider
import com.rarible.core.meta.resource.GatewayProvider
import com.rarible.core.meta.resource.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.RandomGatewayProvider
import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.detector.embedded.DefaultEmbeddedContentDecoderProvider
import com.rarible.core.meta.resource.detector.embedded.EmbeddedBase64Decoder
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetectProcessor
import com.rarible.core.meta.resource.detector.embedded.EmbeddedSvgDecoder
import com.rarible.core.meta.resource.http.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.PropertiesHttpLoader
import com.rarible.core.meta.resource.http.ProxyWebClientBuilder
import com.rarible.core.meta.resource.parser.ArweaveUrlResourceParser
import com.rarible.core.meta.resource.parser.CidUrlResourceParser
import com.rarible.core.meta.resource.parser.DefaultUrlResourceParserProvider
import com.rarible.core.meta.resource.parser.HttpUrlResourceParser
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser
import com.rarible.core.meta.resource.resolver.ArweaveGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsCidGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.SimpleHttpGatewayResolver
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

/**
 * Annotation that allows to disable meta tests locally, since they may require internet connection.
 * To run locally, pass the system property -DRARIBLE_TESTS_RUN_META_TESTS=true or comment out this annotation.
 */
@EnabledIfSystemProperty(named = "RARIBLE_TESTS_RUN_META_TESTS", matches = "true")
annotation class ItemMetaTest

abstract class BasePropertiesResolverTest {

    protected val tokenRepository: TokenRepository = mockk()

    protected val sender = ReadOnlyMonoTransactionSender(
        MonoEthereum(
            WebClientTransport(
                "https://dark-solitary-resonance.quiknode.pro/c0b7c629520de6c3d39261f6417084d71c3f8791/",
                MonoEthereum.mapper(),
                10000,
                10000
            )
        ),
        Address.ZERO()
    )

    val proxyUrl = System.getProperty("RARIBLE_TESTS_OPENSEA_PROXY_URL") ?: ""

    val defaultWebClientBuilder = DefaultWebClientBuilder(followRedirect = false)
    val proxyWebClientBuilder = ProxyWebClientBuilder(
        readTimeout = 10000,
        connectTimeout = 3000,
        proxyUrl = proxyUrl,
        followRedirect = false
    )

    protected val externalHttpClient = ExternalHttpClient(
        openseaUrl = "https://api.opensea.io/api/v1",
        openseaApiKey = "",
        proxyUrl = proxyUrl,
        defaultWebClientBuilder = defaultWebClientBuilder,
        proxyWebClientBuilder = proxyWebClientBuilder,
    )

    protected val polygonExternalHttpClient = ExternalHttpClient(
        openseaUrl = "https://api.opensea.io/api/v2",
        openseaApiKey = "",
        proxyUrl = proxyUrl,
        defaultWebClientBuilder = defaultWebClientBuilder,
        proxyWebClientBuilder = proxyWebClientBuilder,
    )

    protected val publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY.trimEnd('/'))
    private val innerGatewaysProvider = RandomGatewayProvider(IPFS_PUBLIC_GATEWAY.split(",").map { it.trimEnd('/') })

    private val urlResourceProcessor = getUrlResourceProcessor()
    private val urlResolver = getUrlResolver(publicGatewayProvider, innerGatewaysProvider)

    private val embeddedContentDetectProcessor = EmbeddedContentDetectProcessor(
        provider = DefaultEmbeddedContentDecoderProvider(
            embeddedBase64Decoder = EmbeddedBase64Decoder,
            embeddedSvgDecoder = EmbeddedSvgDecoder
        )
    )

    protected val ipfsService = IpfsService(
        parsingProcessor = urlResourceProcessor,
        urlResolver = urlResolver,
        embeddedContentDetectProcessor = embeddedContentDetectProcessor
    )

    protected val tokenUriResolver = BlockchainTokenUriResolver(
        sender = sender,
        tokenRepository = tokenRepository,
        requestTimeout = REQUEST_TIMEOUT
    )

    protected val propertiesHttpLoader = PropertiesHttpLoader(
        externalHttpClient = externalHttpClient,
        defaultRequestTimeout = REQUEST_TIMEOUT,
        openseaRequestTimeout = REQUEST_TIMEOUT
    )

    @BeforeEach
    fun clear() {
        clearMocks(tokenRepository)
    }

    protected fun mockTokenStandard(address: Address, standard: TokenStandard) {
        @Suppress("ReactiveStreamsUnusedPublisher")
        every { tokenRepository.findById(address) } returns Mono.just(
            Token(
                address,
                name = "",
                standard = standard
            )
        )
    }

    private fun getUrlResolver(
        publicGatewayProvider: GatewayProvider,
        innerGatewaysProvider: GatewayProvider
    ): UrlResolver {
        val customGatewaysResolver = LegacyIpfsGatewaySubstitutor(emptyList())
        val arweaveGatewayProvider = ConstantGatewayProvider(ArweaveUrl.ARWEAVE_GATEWAY)

        val ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            innerGatewaysProvider = innerGatewaysProvider,
            customGatewaysResolver = customGatewaysResolver
        )

        val ipfsCidGatewayResolver = IpfsCidGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            innerGatewaysProvider = innerGatewaysProvider,
        )

        val arweaveGatewayResolver = ArweaveGatewayResolver(
            arweaveGatewayProvider = arweaveGatewayProvider
        )

        return UrlResolver(
            ipfsGatewayResolver = ipfsGatewayResolver,
            ipfsCidGatewayResolver = ipfsCidGatewayResolver,
            arweaveGatewayResolver = arweaveGatewayResolver,
            simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
        )
    }

    fun getUrlResourceProcessor(): UrlResourceParsingProcessor {
        val cidOneValidator = CidV1Validator()
        val foreignIpfsUrlResourceParser = ForeignIpfsUrlResourceParser(
            cidOneValidator = cidOneValidator
        )

        val defaultUrlResourceParserProvider = DefaultUrlResourceParserProvider(
            arweaveUrlParser = ArweaveUrlResourceParser(),
            foreignIpfsUrlResourceParser = foreignIpfsUrlResourceParser,
            abstractIpfsUrlResourceParser = AbstractIpfsUrlResourceParser(),
            cidUrlResourceParser = CidUrlResourceParser(cidOneValidator),
            httpUrlParser = HttpUrlResourceParser()
        )

        return UrlResourceParsingProcessor(
            provider = defaultUrlResourceParserProvider
        )
    }

    companion object {
        const val REQUEST_TIMEOUT: Long = 20000
        const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"
        const val CID = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"
    }
}
