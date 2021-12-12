package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.*
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedValueItemReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ReversedItemReducerTest {
    private val reversedItemReducer = ReversedValueItemReducer()

    @Test
    fun `should revert mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE, deleted = false)
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = reversedItemReducer.reduce(item, event)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isEqualTo(true)
    }

    @Test
    fun `should revert burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ZERO, deleted = true)
        val event = createRandomBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = reversedItemReducer.reduce(item, event)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.deleted).isEqualTo(false)
    }
}
