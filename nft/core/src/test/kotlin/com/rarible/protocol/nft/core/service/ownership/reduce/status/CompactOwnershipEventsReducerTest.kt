package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.data.createRandomOwnershipChangeLazyValueEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferFromEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.OwnershipEvent
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

class CompactOwnershipEventsReducerTest {
    private val featureFlags = FeatureFlags(compactRevertableEvents = true)
    private val properties = NftIndexerProperties.ReduceProperties(maxRevertableEventsAmount = 1)

    private val reducer = CompactOwnershipEventsReducer(
        featureFlags,
        properties
    )

    @Test
    fun `compact item events`() = runBlocking<Unit> {
        val events = listOf(
            createRandomOwnershipTransferToEvent(value = 2, blockNumber = 1),
            createRandomOwnershipTransferFromEvent(value = 2, blockNumber = 1),
            createRandomOwnershipTransferToEvent(value = 8, blockNumber = 1),
            createRandomOwnershipTransferFromEvent(value = 8, blockNumber = 1),

            createRandomOwnershipTransferToEvent(value = 1, blockNumber = 2),
            createRandomOwnershipTransferToEvent(value = 1, blockNumber = 2),
            createRandomOwnershipTransferFromEvent(value = 2, blockNumber = 3),
            createRandomOwnershipChangeLazyValueEvent(),
        )
        val ownership = createRandomOwnership().withRevertableEvents(events)

        val reduced = reducer.reduce(ownership, mockk())
        val compactEvents = reduced.revertableEvents
        Assertions.assertThat(compactEvents).hasSize(5)

        Assertions.assertThat((compactEvents[0] as OwnershipEvent.TransferToEvent).value.value).isEqualTo(BigInteger("10"))
        Assertions.assertThat((compactEvents[1] as OwnershipEvent.TransferFromEvent).value.value).isEqualTo(BigInteger("10"))
        Assertions.assertThat((compactEvents[2] as OwnershipEvent.TransferToEvent).value.value).isEqualTo(BigInteger("2"))
        Assertions.assertThat((compactEvents[3] as OwnershipEvent.TransferFromEvent).value.value).isEqualTo(BigInteger("2"))
        Assertions.assertThat(compactEvents[4]).isInstanceOf(OwnershipEvent.ChangeLazyValueEvent::class.java)

        val reReduced = reducer.reduce(reduced, mockk())
        Assertions.assertThat(reReduced).isEqualTo(reReduced)
    }
}