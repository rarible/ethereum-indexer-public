package com.rarible.protocol.nft.core.service.policy

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.repository.data.createAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InactiveEventApplyPolicyTest {
    private val inactiveEventApplyPolicy = InactiveEventApplyPolicy<ItemEvent>()

    @Test
    fun `should remove pending log`() {
        val address = createAddress()

        val pendingLog = createRandomMintItemEvent().withNewValues(
            status = EthereumLogStatus.PENDING,
            blockNumber = null,
            minorLogIndex = 1,
            address = address
        )
        val inactiveLog = pendingLog.withNewValues(
            status = EthereumLogStatus.INACTIVE
        )
        val events = (1..3).map {
            createRandomMintItemEvent().withNewValues(
                status = EthereumLogStatus.PENDING,
                blockNumber = null,
                minorLogIndex = 1,
                address = address
            )
        }

        val wasApplied = inactiveEventApplyPolicy.wasApplied(events + pendingLog, inactiveLog)
        assertThat(wasApplied).isTrue

        val reduced = inactiveEventApplyPolicy.reduce(events + pendingLog, inactiveLog)
        assertThat(reduced).isEqualTo(events)
    }
}
