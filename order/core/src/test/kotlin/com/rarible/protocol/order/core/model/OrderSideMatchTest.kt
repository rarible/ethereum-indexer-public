package com.rarible.protocol.order.core.model

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBytes
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

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

}