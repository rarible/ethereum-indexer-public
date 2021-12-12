package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.*
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardValueItemReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class BlockchainItemReducerTest {
    private val itemBlockchainItemReducer = ForwardValueItemReducer()

    @Test
    fun `should reduce mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ZERO, deleted = true)
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = itemBlockchainItemReducer.reduce(item, event)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.deleted).isEqualTo(false)
    }

    @Test
    fun `should reduce burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE, deleted = false)
        val event = createRandomBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = itemBlockchainItemReducer.reduce(item, event)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isEqualTo(true)
    }

    companion object {
        @JvmStatic
        fun invalidReduceEvents() = Stream.of(createRandomLazyMintItemEvent(), createRandomLazyBurnItemEvent())
    }
}
