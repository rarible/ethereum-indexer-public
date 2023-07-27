package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.data.withNewValues
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CompositeEventTest {
    @Test
    fun `get min event from item event`() {
        val itemEvent1 = createRandomMintItemEvent()
            .withNewValues(
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1L,
                logIndex = 1,
                minorLogIndex = 1,
            )
        val itemEvent2 = createRandomMintItemEvent()
            .withNewValues(
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 2L,
                logIndex = 1,
                minorLogIndex = 1,
            )

        val composeEvent1 = CompositeEvent(itemEvent1, emptyList())
        val composeEvent2 = CompositeEvent(itemEvent2, emptyList())

        assertThat(minOf(composeEvent1, composeEvent2)).isEqualTo(composeEvent1)
        assertThat(minOf(composeEvent2, composeEvent1)).isEqualTo(composeEvent1)
    }

    @Test
    fun `get min event from item ownership`() {
        val ownershipEvent1 = createRandomOwnershipTransferToEvent()
            .withNewValues(
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1L,
                logIndex = 1,
                minorLogIndex = 1,
            )
        val ownershipEvent2 = createRandomOwnershipTransferToEvent()
            .withNewValues(
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1L,
                logIndex = 1,
                minorLogIndex = 2,
            )

        val composeEvent1 = CompositeEvent(null, listOf(ownershipEvent1))
        val composeEvent2 = CompositeEvent(null, listOf(ownershipEvent2))

        assertThat(minOf(composeEvent1, composeEvent2)).isEqualTo(composeEvent1)
        assertThat(minOf(composeEvent2, composeEvent1)).isEqualTo(composeEvent1)
    }
}
