package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PendingOwnersItemReducerTest {
    private val pendingOwnersItemReducer = PendingOwnersItemReducer()

    @Test
    fun `should add pending owner with zero value on mint`() = runBlocking<Unit> {
        val owner = randomAddress()
        val event = createRandomMintItemEvent().copy(owner = owner, supply = EthUInt256.TEN)
        val item = createRandomItem().copy(ownerships = emptyMap())

        val reducedItem = pendingOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should add pending owner with zero value on transfer`() = runBlocking<Unit> {
        val owner = randomAddress()
        val event = createRandomTransferItemEvent().copy(to = owner, value = EthUInt256.TEN)
        val item = createRandomItem().copy(ownerships = emptyMap())

        val reducedItem = pendingOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should not change owner value on mint`() = runBlocking<Unit> {
        val owner = randomAddress()
        val event = createRandomMintItemEvent().copy(owner = owner, supply = EthUInt256.TEN)
        val item = createRandomItem().copy(ownerships = mapOf(owner to EthUInt256.ONE))

        val reducedItem = pendingOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should not change owner value on transfer`() = runBlocking<Unit> {
        val owner = randomAddress()
        val event = createRandomTransferItemEvent().copy(to = owner, value = EthUInt256.TEN)
        val item = createRandomItem().copy(ownerships = mapOf(owner to EthUInt256.ONE))

        val reducedItem = pendingOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.ONE)
    }
}
