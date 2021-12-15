package com.rarible.protocol.nft.core.service.ownership.reduce.lazy

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnershipLazyTransferToEvent
import com.rarible.protocol.nft.core.repository.data.createOwnership
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LazyOwnershipReducerTest {
    private val lazyOwnershipReducer = LazyOwnershipReducer()

    @Test
    fun `should handle lazy event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(
            lazyValue = EthUInt256.ZERO,
            lastLazyEventTimestamp = null
        )
        val event = createRandomOwnershipLazyTransferToEvent().copy(
            value = EthUInt256.TEN
        )
        val reducedOwnership = lazyOwnershipReducer.reduce(ownership, event)
        assertThat(reducedOwnership.lazyValue).isEqualTo(EthUInt256.TEN)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
        assertThat(reducedOwnership.lastLazyEventTimestamp).isEqualTo(event.timestamp)
    }

    @Test
    fun `should not handle lazy event if it is from past`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(
            lazyValue = EthUInt256.ZERO,
            value = EthUInt256.ONE,
            lastLazyEventTimestamp = 10
        )
        val event = createRandomOwnershipLazyTransferToEvent().copy(
            value = EthUInt256.TEN,
            timestamp = 1
        )
        val reducedOwnership = lazyOwnershipReducer.reduce(ownership, event)
        assertThat(reducedOwnership.lazyValue).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.ONE)
    }
}
