package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.log
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.service.descriptors.erc721.ERC721TransferLogDescriptor
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.domain.response.Transaction
import java.math.BigInteger

internal class MintPriceTest {

    @Nested
    inner class OrderVersionFetchWithCorrectLimitFt {

        private val tokenRegistrationService = mockk<TokenRegistrationService>()
        private val customMintDetector = mockk<CustomMintDetector>()
        private val ignoredTokenResolver = mockk<IgnoredTokenResolver>()
        private val indexerProperties = mockk<NftIndexerProperties>()
        private lateinit var descriptor: ERC721TransferLogDescriptor

        init {
            every { tokenRegistrationService.getTokenStandard(any()) } returns Mono.just(TokenStandard.ERC721)
            every { customMintDetector.isErc721Mint(any(), any()) } returns true
            every { ignoredTokenResolver.resolve() } returns emptySet()
            every { indexerProperties.scannerProperties } returns NftIndexerProperties.ScannerProperties()
        }

        @BeforeEach
        fun setUp() {
            descriptor = ERC721TransferLogDescriptor(
                tokenRegistrationService, customMintDetector, ignoredTokenResolver, indexerProperties
            )
        }

        @Test
        fun `should have mintPrice for 1 minted item`() = runBlocking<Unit> {
            val value = 30.toBigInteger()
            val log = log(
                listOf(
                    Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                    Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    Word.apply("0x0000000000000000000000003007b29381d3543f3743afec9a38e27bc729d65a"),
                    Word.apply("0x0000000000000000000000000000000000000000000000000000000000000488")
                ),
                "0x",
                address = randomAddress(),
            )

            val event = descriptor.convert(log, txWithValue(value), 0, 0, 1).awaitSingle()

            assertThat(event.mintPrice).isEqualTo(value)
        }

        @Test
        fun `should have mintPrice for 2 minted item`() = runBlocking<Unit> {
            val value = 30.toBigInteger()
            val log = log(
                listOf(
                    Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                    Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    Word.apply("0x0000000000000000000000003007b29381d3543f3743afec9a38e27bc729d65a"),
                    Word.apply("0x0000000000000000000000000000000000000000000000000000000000000488")
                ),
                "0x",
                address = randomAddress(),
            )

            val event = descriptor.convert(log, txWithValue(value), 0, 0, 2).awaitSingle()

            assertThat(event.mintPrice).isEqualTo(15.toBigInteger())
        }

        @Test
        fun `shouldn't have mintPrice`() = runBlocking<Unit> {
            val log = log(
                listOf(
                    Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                    Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    Word.apply("0x0000000000000000000000003007b29381d3543f3743afec9a38e27bc729d65a"),
                    Word.apply("0x0000000000000000000000000000000000000000000000000000000000000488")
                ),
                "0x",
                address = randomAddress(),
            )

            val event = descriptor.convert(log, txWithValue(BigInteger.ZERO), 0, 0, 1).awaitSingle()

            assertThat(event.mintPrice).isEqualTo(BigInteger.ZERO)
        }
    }

    private fun txWithValue(value: BigInteger) : Transaction {
        val transaction = mockk<Transaction>()
        every { transaction.value() } returns value
        return transaction
    }
}
