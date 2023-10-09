package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.meta.resource.http.DefaultHttpClient
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.OpenseaHttpClient
import com.rarible.core.meta.resource.http.ProxyHttpClient
import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.builder.ProxyWebClientBuilder
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.resolver.RandomGatewayProvider
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.cache.IpfsContentCache
import com.rarible.protocol.nft.core.service.item.meta.cache.RawPropertiesCacheService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
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

    private val defaultWebClientBuilder = DefaultWebClientBuilder(followRedirect = true)
    private val proxyWebClientBuilder = ProxyWebClientBuilder(
        readTimeout = 10000,
        connectTimeout = 3000,
        proxyUrl = proxyUrl,
        followRedirect = true
    )

    private val defaultHttpClient = DefaultHttpClient(
        builder = defaultWebClientBuilder,
        requestTimeout = REQUEST_TIMEOUT
    )
    private val proxyHttpClient = ProxyHttpClient(
        builder = proxyWebClientBuilder,
        requestTimeout = REQUEST_TIMEOUT
    )

    private val openseaHttpClient = OpenseaHttpClient(
        builder = proxyWebClientBuilder,
        requestTimeout = REQUEST_TIMEOUT,
        openseaUrl = openseaUrl,
        openseaApiKey = openseaApiKey,
        proxyUrl = proxyUrl,
    )

    protected val externalHttpClient = ExternalHttpClient(
        defaultClient = defaultHttpClient,
        proxyClient = proxyHttpClient,
        customClients = listOf(openseaHttpClient)
    )

    protected val urlParser = UrlParser()

    protected val publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY.trimEnd('/'))
    private val internalGatewaysProvider = RandomGatewayProvider(IPFS_PUBLIC_GATEWAY.split(",").map { it.trimEnd('/') })
    private val urlResolver = UrlResolver(
        ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            internalGatewayProvider = internalGatewaysProvider,
            customGatewaysResolver = LegacyIpfsGatewaySubstitutor(emptyList()) // For handle Legacy gateways
        )
    )

    protected val urlService = UrlService(
        urlParser = urlParser,
        urlResolver = urlResolver,
    )

    protected val properties = mockk<NftIndexerProperties> {
        every { requestTimeout } returns REQUEST_TIMEOUT
    }

    protected val tokenUriResolver = BlockchainTokenUriResolver(
        sender = sender,
        tokenRepository = tokenRepository,
        properties = properties,
    )

    protected val ipfsContentCache: IpfsContentCache = mockk()

    protected val featureFlags: FeatureFlags = FeatureFlags(enableMetaRawPropertiesCache = false)

    protected val rawPropertiesProvider = RawPropertiesProvider(
        rawPropertiesCacheService = RawPropertiesCacheService(
            caches = listOf(
                ipfsContentCache
            )
        ),
        urlService = urlService,
        externalHttpClient = externalHttpClient,
        featureFlags = featureFlags
    )

    protected val rariblePropertiesResolver = RariblePropertiesResolver(
        urlService = urlService,
        rawPropertiesProvider = rawPropertiesProvider,
        tokenUriResolver = tokenUriResolver
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

    companion object {
        const val REQUEST_TIMEOUT: Long = 20000
        const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"
        const val CID = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"
        const val openseaUrl = "https://api.opensea.io/api/v1"
        const val openseaApiKey = ""
        val proxyUrl = System.getProperty("RARIBLE_TESTS_OPENSEA_PROXY_URL") ?: ""
    }
}
