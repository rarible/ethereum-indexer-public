package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
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

    @BeforeEach
    fun clear() {
        clearMocks(tokenRepository)
    }

    fun createSender() = ReadOnlyMonoTransactionSender(
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

    fun mockTokenStandard(address: Address, standard: TokenStandard) {
        @Suppress("ReactiveStreamsUnusedPublisher")
        every { tokenRepository.findById(address) } returns Mono.just(
            Token(
                address,
                name = "",
                standard = standard
            )
        )
    }

}
