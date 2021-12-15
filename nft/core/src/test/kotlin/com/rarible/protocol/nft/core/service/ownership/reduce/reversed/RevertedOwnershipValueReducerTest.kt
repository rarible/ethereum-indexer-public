package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferFromEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.repository.data.createOwnership
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.RevertedOwnershipValueReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class RevertedOwnershipValueReducerTest {
    private val revertedOwnershipValueReducer = RevertedOwnershipValueReducer()

    @Test
    fun `should calculate value for revert transfer to event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.of(11))
        val transferTo = createRandomOwnershipTransferToEvent().copy(value = EthUInt256.ONE)

        val reducedOwnership = revertedOwnershipValueReducer.reduce(ownership, transferTo)
        Assertions.assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should calculate value for revert transfer from event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.of(9))
        val transferFrom = createRandomOwnershipTransferFromEvent().copy(value = EthUInt256.ONE)

        val reducedOwnership = revertedOwnershipValueReducer.reduce(ownership, transferFrom)
        Assertions.assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should not change value for self revert transfer from event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.TEN)
        val transferFrom = createRandomOwnershipTransferFromEvent().copy(to = ownership.owner, value = EthUInt256.ONE)

        val reducedOwnership = revertedOwnershipValueReducer.reduce(ownership, transferFrom)
        Assertions.assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should not change value for self revert transfer to event`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(value = EthUInt256.TEN)
        val transferFrom = createRandomOwnershipTransferToEvent().copy(from = ownership.owner, value = EthUInt256.ONE)

        val reducedOwnership = revertedOwnershipValueReducer.reduce(ownership, transferFrom)
        Assertions.assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
    }
}
