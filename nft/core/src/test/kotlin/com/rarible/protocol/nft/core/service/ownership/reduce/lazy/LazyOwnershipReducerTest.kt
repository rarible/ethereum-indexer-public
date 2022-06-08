package com.rarible.protocol.nft.core.service.ownership.reduce.lazy

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.data.createRandomOwnershipLazyBurnEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipLazyTransferToEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class LazyOwnershipReducerTest {
    private val lazyOwnershipReducer = LazyOwnershipReducer()

    @Test
    fun `should handle lazy event`() = runBlocking<Unit> {
        val ownership = createRandomOwnership().copy(
            lazyValue = EthUInt256.ZERO,
            lastLazyEventTimestamp = null
        )
        val event = createRandomOwnershipLazyTransferToEvent().copy(
            value = EthUInt256.TEN
        )
        val reducedOwnership = reduce(ownership, event)
        assertThat(reducedOwnership.lazyValue).isEqualTo(EthUInt256.TEN)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.TEN)
        assertThat(reducedOwnership.lastLazyEventTimestamp).isEqualTo(event.timestamp)
    }

    @Test
    fun `should handle burn lazy event`() = runBlocking<Unit> {
        val ownership = createRandomOwnership().copy(
            lazyValue = EthUInt256.ZERO,
            lastLazyEventTimestamp = null
        )
        val mintEvent = createRandomOwnershipLazyTransferToEvent().copy(
            value = EthUInt256.TEN
        ).withNewValues(createdAt = Instant.now() - Duration.ofDays(1))
        val burnEvent = createRandomOwnershipLazyBurnEvent().copy(
            value = EthUInt256.TEN
        ).withNewValues(createdAt = Instant.now())
        val reducedOwnership = reduce(ownership, mintEvent, burnEvent)
        assertThat(reducedOwnership.lazyValue).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedOwnership.lastLazyEventTimestamp).isEqualTo(burnEvent.timestamp)
    }

    @Test
    fun `should handle save real minted value after burn lazy event`() = runBlocking<Unit> {
        val ownership = createRandomOwnership().copy(
            value = EthUInt256.TEN,
            lazyValue = EthUInt256.of(9),
            lastLazyEventTimestamp = null
        )
        val burnEvent = createRandomOwnershipLazyBurnEvent().copy(
            value = EthUInt256.TEN
        ).withNewValues(createdAt = Instant.now())

        val reducedOwnership = reduce(ownership, burnEvent)
        assertThat(reducedOwnership.lazyValue).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.ONE)
        assertThat(reducedOwnership.lastLazyEventTimestamp).isEqualTo(burnEvent.timestamp)
    }

    @Test
    fun `should not handle lazy event if it is from past`() = runBlocking<Unit> {
        val ownership = createRandomOwnership().copy(
            lazyValue = EthUInt256.ZERO,
            value = EthUInt256.ONE,
            lastLazyEventTimestamp = 10
        )
        val event = createRandomOwnershipLazyTransferToEvent().copy(
            value = EthUInt256.TEN
        ).let {
            it.copy(log = it.log.copy(createdAt = Instant.EPOCH))
        }
        val reducedOwnership = reduce(ownership, event)
        assertThat(reducedOwnership.lazyValue).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.ONE)
    }

    private suspend fun reduce(ownership: Ownership, vararg events: OwnershipEvent): Ownership {
        return events.fold(ownership) { entity, event ->
            lazyOwnershipReducer.reduce(entity, event)
        }
    }
}
