package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferFromEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.repository.data.createOwnership
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ForwardOwnershipValueReducerTest {
    private val forwardOwnershipValueReducer = ForwardOwnershipValueReducer()

    @Test
    fun `should calculate value for transfer to event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.ONE)
        val transferTo = createRandomOwnershipTransferToEvent().copy(value = EthUInt256.of(9))

        val reducedOwnership = forwardOwnershipValueReducer.reduce(ownership, transferTo)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should calculate value for transfer from event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.of(11))
        val transferFrom = createRandomOwnershipTransferFromEvent().copy(value = EthUInt256.ONE)

        val reducedOwnership = forwardOwnershipValueReducer.reduce(ownership, transferFrom)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should not change value for self transfer from event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.TEN)
        val transferFrom = createRandomOwnershipTransferFromEvent().copy(to = ownership.owner, value = EthUInt256.ONE)

        val reducedOwnership = forwardOwnershipValueReducer.reduce(ownership, transferFrom)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should not change value for self transfer to event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.TEN)
        val transferFrom = createRandomOwnershipTransferToEvent().copy(from = ownership.owner, value = EthUInt256.ONE)

        val reducedOwnership = forwardOwnershipValueReducer.reduce(ownership, transferFrom)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }
}
