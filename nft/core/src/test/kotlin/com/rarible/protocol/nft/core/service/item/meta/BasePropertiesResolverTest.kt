package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PropertiesHttpLoader
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

    protected val externalHttpClient = ExternalHttpClient(
        openseaUrl = "https://api.opensea.io/api/v1",
        openseaApiKey = "",
        readTimeout = 10000,
        connectTimeout = 3000,
        proxyUrl = System.getProperty("RARIBLE_TESTS_OPENSEA_PROXY_URL") ?: ""
    )

    protected val ipfsService = IpfsService(
        NftIndexerProperties.IpfsProperties(
            IPFS_PUBLIC_GATEWAY,
            IPFS_PUBLIC_GATEWAY
        )
    )

    protected val tokenUriResolver = BlockchainTokenUriResolver(
        sender = sender,
        tokenRepository = tokenRepository,
        requestTimeout = REQUEST_TIMEOUT
    )

    protected val propertiesHttpLoader = PropertiesHttpLoader(
        externalHttpClient = externalHttpClient,
        requestTimeout = REQUEST_TIMEOUT
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

    protected companion object {
        const val REQUEST_TIMEOUT: Long = 20000
        const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"
    }
}
