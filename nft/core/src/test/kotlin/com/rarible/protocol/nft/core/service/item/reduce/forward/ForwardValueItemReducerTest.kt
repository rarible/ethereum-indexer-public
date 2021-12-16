package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ForwardValueItemReducerTest {
    private val forwardValueItemReducer = ForwardValueItemReducer()

    @Test
    fun `should calculate supply on mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.of(9))

        val reducedItem = forwardValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should calculate supply on burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.of(11))
        val event = createRandomBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = forwardValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
    }
}
