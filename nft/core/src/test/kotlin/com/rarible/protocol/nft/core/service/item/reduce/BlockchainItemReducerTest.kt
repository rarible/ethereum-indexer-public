package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomLazyBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomLazyMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardValueItemReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.stream.Stream

internal class BlockchainItemReducerTest {
    private val itemBlockchainItemReducer = ForwardValueItemReducer()

    @Test
    fun `should reduce mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ZERO, deleted = true)
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = itemBlockchainItemReducer.reduce(item, event)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should reduce burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE, deleted = false)
        val event = createRandomBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = itemBlockchainItemReducer.reduce(item, event)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
    }

    companion object {
        @JvmStatic
        fun invalidReduceEvents() = Stream.of(createRandomLazyMintItemEvent(), createRandomLazyBurnItemEvent())
    }
}
