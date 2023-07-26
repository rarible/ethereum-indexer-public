package com.rarible.protocol.order.core.model

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBytes
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.gemswap.v1.GemSwapV1
import com.rarible.protocol.contracts.exchange.wrapper.ExchangeWrapper
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.response.Transaction

internal class OrderSideMatchTest {

    @Test
    fun `should set marketplace marker`() {
        val match = OrderSideMatch(
            Word.apply(randomWord()),
            Word.apply(randomWord()),
            OrderSide.LEFT,
            EthUInt256.ONE,
            randomAddress(),
            randomAddress(),
            randomErc721(),
            randomErc20(),
            null, null, null, null, null, null,
            adhoc = true
        )

        val marker = Binary.apply(randomBytes(24)).add(OrderSideMatch.MARKER_BYTES)
        val result = OrderSideMatch.addMarketplaceMarker(listOf(match), marker, null)[0]

        Assertions.assertThat(result.marketplaceMarker).isEqualTo(
            Word.apply(marker)
        )
    }

    @Test
    fun `should set counter marketplace marker`() {
        val match = OrderSideMatch(
            Word.apply(randomWord()),
            Word.apply(randomWord()),
            OrderSide.LEFT,
            EthUInt256.ONE,
            randomAddress(),
            randomAddress(),
            randomErc721(),
            randomErc20(),
            null, null, null, null, null, null,
            counterAdhoc = true
        )

        val marker = Binary.apply(randomBytes(24)).add(OrderSideMatch.MARKER_BYTES)
        val result = OrderSideMatch.addMarketplaceMarker(listOf(match), marker, null)[0]

        Assertions.assertThat(result.counterMarketplaceMarker).isEqualTo(
            Word.apply(marker)
        )
    }

    @Test
    fun `get real taker - from transaction`() {
        val expectedTaker = randomAddress()
        listOf(
            GemSwapV1.batchBuyWithETHSignature().id(),
            GemSwapV1.batchBuyWithERC20sSignature().id(),
            ExchangeWrapper.singlePurchaseSignature().id(),
            ExchangeWrapper.bulkPurchaseSignature().id(),
            ExchangeWrapper.bulkPurchaseSignature().id(),
            Binary.apply("0x86496e7a") // singlePurchaseSignature
        ).forEachIndexed { index, methodId ->
            val transaction = mockk<Transaction>() {
                every { input() } returns methodId
                every { from() } returns expectedTaker
            }
            val realTaker = OrderSideMatch.getRealTaker(randomAddress(), transaction)
            assertThat(realTaker).isEqualTo(expectedTaker).withFailMessage { "Fail for $index method" }
        }
    }
}
