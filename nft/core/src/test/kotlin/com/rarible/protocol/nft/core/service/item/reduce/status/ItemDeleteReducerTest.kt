package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.withNewValues
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ItemDeleteReducerTest {
    val reducer = ItemDeleteReducer()

    @Test
    fun `should mark item as deleted`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            deleted = false,
            supply = EthUInt256.ZERO,
            lazySupply = EthUInt256.ZERO,
            revertableEvents = listOf(
                createRandomBurnItemEvent().withNewValues(status = Log.Status.PENDING)
            )
        )
        val reducedItem = reducer.reduce(item, createRandomMintItemEvent())
        assertThat(reducedItem.deleted).isTrue
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
    }
}
