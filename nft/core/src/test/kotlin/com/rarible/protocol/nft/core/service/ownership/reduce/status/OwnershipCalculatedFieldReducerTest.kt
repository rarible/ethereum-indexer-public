package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.data.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

internal class OwnershipCalculatedFieldReducerTest {
    val reducer = OwnershipCalculatedFieldReducer()

    @Test
    fun `should get the latest confirmed log for updatedAt field`() = runBlocking<Unit> {
        val now = nowMillis()
        val event1 = createRandomOwnershipTransferToEvent().withNewValues(status = EthereumLogStatus.CONFIRMED, createdAt = now - Duration.ofMinutes(11))
        val event2 = createRandomOwnershipTransferToEvent().withNewValues(status = EthereumLogStatus.CONFIRMED, createdAt = now - Duration.ofMinutes(1))
        val event3 = createRandomOwnershipTransferToEvent().withNewValues(status = EthereumLogStatus.PENDING, createdAt = now)

        val ownership = createRandomOwnership().copy(
            revertableEvents = listOf(event1, event2, event3)
        )
        val reducedOwnership = reducer.reduce(ownership, event3)
        Assertions.assertThat(reducedOwnership.date).isEqualTo(event2.log.createdAt)
    }

    @Test
    fun `should get the latest pending log for updatedAt field`() = runBlocking<Unit> {
        val now = nowMillis()
        val event1 = createRandomOwnershipTransferToEvent().withNewValues(status = EthereumLogStatus.PENDING, createdAt = now - Duration.ofMinutes(1))
        val event2 = createRandomOwnershipTransferToEvent().withNewValues(status = EthereumLogStatus.PENDING, createdAt = now)

        val ownership = createRandomOwnership().copy(
            revertableEvents = listOf(event1, event2)
        )
        val reducedOwnership = reducer.reduce(ownership, event2)
        Assertions.assertThat(reducedOwnership.date).isEqualTo(event2.log.createdAt)
    }

    @Test
    fun `should not change updateAt field if event list is empty`() = runBlocking<Unit> {
        val ownership = createRandomOwnership().copy(
            revertableEvents = emptyList()
        )
        val reducedOwnership = reducer.reduce(ownership, createRandomOwnershipTransferToEvent())
        Assertions.assertThat(reducedOwnership.date).isEqualTo(ownership.date)
    }
}