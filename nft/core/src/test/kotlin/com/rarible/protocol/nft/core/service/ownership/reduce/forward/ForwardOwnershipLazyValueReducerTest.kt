package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnershipChangeLazyValueEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.repository.data.createOwnership
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

internal class ForwardOwnershipLazyValueReducerTest {
    private val forwardOwnershipLazyValueReducer = ForwardOwnershipLazyValueReducer()

    @Test
    fun `should calculate value and lazyValue`() = runBlocking<Unit> {
        val event = createRandomOwnershipChangeLazyValueEvent().copy(value = EthUInt256.ONE)
        val ownership = createOwnership().copy(value = EthUInt256.of(11), lazyValue = EthUInt256.of(6))

        val reducedEvent = forwardOwnershipLazyValueReducer.reduce(ownership, event)
        assertThat(reducedEvent.value).isEqualTo(EthUInt256.TEN)
        assertThat(reducedEvent.lazyValue).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should calculate value and lazyValue for lazy owner mint`() = runBlocking<Unit> {
        val event = createRandomOwnershipTransferToEvent().copy(value = EthUInt256.ONE, from = Address.ZERO())
        val ownership = createOwnership().copy(value = EthUInt256.of(11), lazyValue = EthUInt256.of(6))

        val reducedEvent = forwardOwnershipLazyValueReducer.reduce(ownership, event)
        assertThat(reducedEvent.value).isEqualTo(EthUInt256.TEN)
        assertThat(reducedEvent.lazyValue).isEqualTo(EthUInt256.of(5))
    }
}
