package com.rarible.protocol.nft.core.service.policy

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.model.ItemEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PendingEventApplyPolicyTest {
    private val pendingEventApplyPolicy = PendingEventApplyPolicy<ItemEvent>()

    @Test
    fun `should add pending log to list`() {
        val pendingEvent = createRandomMintItemEvent().withNewValues(
            status = EthereumLogStatus.PENDING,
            blockNumber = null,
            minorLogIndex = 1
        )
        val events = (1L..3).map {
            createRandomMintItemEvent().withNewValues(
                status = EthereumLogStatus.CONFIRMED,
                blockNumber = it,
                minorLogIndex = 1
            )
        }
        val wasApplied = pendingEventApplyPolicy.wasApplied(events, pendingEvent)
        assertThat(wasApplied).isFalse

        val reduced = pendingEventApplyPolicy.reduce(events, pendingEvent)
        assertThat(reduced).isEqualTo(events + pendingEvent)
    }

    @Test
    fun `should not apply the same pending log`() {
        val pendingEvent = createRandomMintItemEvent().withNewValues(
            status = EthereumLogStatus.PENDING,
            blockNumber = null,
            minorLogIndex = 1
        )
        val events = (1L..3).map {
            createRandomMintItemEvent().withNewValues(
                status = EthereumLogStatus.CONFIRMED,
                blockNumber = it,
                minorLogIndex = 1
            )
        }
        val wasApplied = pendingEventApplyPolicy.wasApplied(events + pendingEvent + events, pendingEvent)
        assertThat(wasApplied).isTrue
    }
}
