package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class ReversedValueItemReducerTest {
    private val reversedValueItemReducer = ReversedValueItemReducer()

    @Test
    fun `should calculate supply on revert mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.of(11))
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = reversedValueItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should calculate supply on revert burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.of(9))
        val event = createRandomBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = reversedValueItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
    }
}
