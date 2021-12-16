package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnershipChangeLazyValueEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.repository.data.createOwnership
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.RevertedOwnershipLazyValueReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.Address

internal class RevertedOwnershipLazyValueReducerTest {
    private val revertedOwnershipLazyValueReducer = RevertedOwnershipLazyValueReducer()

    @Test
    fun `should calculate value and lazyValue`() = runBlocking<Unit> {
        val event = createRandomOwnershipChangeLazyValueEvent().copy(value = EthUInt256.ONE)
        val ownership = createOwnership().copy(value = EthUInt256.of(9), lazyValue = EthUInt256.of(4))

        val reducedEvent = revertedOwnershipLazyValueReducer.reduce(ownership, event)
        Assertions.assertThat(reducedEvent.value).isEqualTo(EthUInt256.TEN)
        Assertions.assertThat(reducedEvent.lazyValue).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should calculate value and lazyValue for lazy owner mint`() = runBlocking<Unit> {
        val event = createRandomOwnershipTransferToEvent().copy(value = EthUInt256.ONE, from = Address.ZERO())
        val ownership = createOwnership().copy(value = EthUInt256.of(9), lazyValue = EthUInt256.of(4))

        val reducedEvent = revertedOwnershipLazyValueReducer.reduce(ownership, event)
        Assertions.assertThat(reducedEvent.value).isEqualTo(EthUInt256.TEN)
        Assertions.assertThat(reducedEvent.lazyValue).isEqualTo(EthUInt256.of(5))
    }
}
