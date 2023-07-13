package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class CompactItemEventsReducerTest {
    private val featureFlags = FeatureFlags(compactRevertableEvents = true)
    private val properties = NftIndexerProperties.ReduceProperties(maxRevertableEventsAmount = 1)

    private val reducer = CompactItemEventsReducer(
        featureFlags,
        properties
    )

    @Test
    fun `compact item events`() = runBlocking<Unit> {
        val events = listOf(
            createRandomMintItemEvent(supply = 2, blockNumber = 1),
            createRandomBurnItemEvent(supply = 2, blockNumber = 1),
            createRandomMintItemEvent(supply = 8, blockNumber = 1),
            createRandomBurnItemEvent(supply = 8, blockNumber = 1),

            createRandomMintItemEvent(supply = 1, blockNumber = 2),
            createRandomMintItemEvent(supply = 1, blockNumber = 2),
            createRandomBurnItemEvent(supply = 2, blockNumber = 3),
            createRandomTransferItemEvent(),
        )
        val item = createRandomItem().withRevertableEvents(events)

        val reduced = reducer.reduce(item, createRandomTransferItemEvent())
        val compactEvents = reduced.revertableEvents
        assertThat(compactEvents).hasSize(5)

        assertThat((compactEvents[0] as ItemEvent.ItemMintEvent).supply.value).isEqualTo(BigInteger("10"))
        assertThat((compactEvents[1] as ItemEvent.ItemBurnEvent).supply.value).isEqualTo(BigInteger("10"))
        assertThat((compactEvents[2] as ItemEvent.ItemMintEvent).supply.value).isEqualTo(BigInteger("2"))
        assertThat((compactEvents[3] as ItemEvent.ItemBurnEvent).supply.value).isEqualTo(BigInteger("2"))
        assertThat(compactEvents[4]).isInstanceOf(ItemEvent.ItemTransferEvent::class.java)

        val reReduced = reducer.reduce(reduced, createRandomTransferItemEvent())
        assertThat(reReduced).isEqualTo(reReduced)
    }
}