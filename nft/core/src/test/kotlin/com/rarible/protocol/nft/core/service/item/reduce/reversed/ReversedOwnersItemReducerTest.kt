package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import com.rarible.protocol.nft.core.repository.data.createAddress
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.Address

internal class ReversedOwnersItemReducerTest {
    private val reversedOwnersItemReducer = ReversedOwnersItemReducer()

    @Test
    fun `should calculate correct owners value for transfer`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.of(5)

        val ownership = mapOf(
            from to EthUInt256.of(5),
            to to EthUInt256.of(6)
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(2)
        Assertions.assertThat(reducedItem.ownerships[to]).isEqualTo(EthUInt256.ONE)
        Assertions.assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `remove owner with zero value`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.of(6)

        val ownership = mapOf(
            from to EthUInt256.of(4),
            to to EthUInt256.of(6)
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(1)
        Assertions.assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should do nothing if transfer with zero`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.ZERO

        val ownership = mapOf(
            from to EthUInt256.of(4),
            to to EthUInt256.of(6)
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(2)
        Assertions.assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.of(4))
        Assertions.assertThat(reducedItem.ownerships[to]).isEqualTo(EthUInt256.of(6))
    }

    @Test
    fun `should do nothing if transfer with zero and to add owner`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.ZERO

        val ownership = mapOf(
            from to EthUInt256.of(4),
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(1)
        Assertions.assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.of(4))
    }

    @Test
    fun `should do nothing if transfer with zero and to add owners`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.ZERO

        val ownership = emptyMap<Address, EthUInt256>()

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(0)
    }

    @Test
    fun `should remove owner from map after revert mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = mapOf(owner to supply)

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(0)
    }

    @Test
    fun `should calculate owner value for revert mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = mapOf(
            owner to EthUInt256.of(25),
        )

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(1)
        Assertions.assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.of(15))
    }

    @Test
    fun `should do nothing if zero revert mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ZERO

        val ownership = emptyMap<Address, EthUInt256>()

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(0)
    }

    @Test
    fun `should not change owner value if zero revert mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ZERO

        val ownership = mapOf(
            owner to EthUInt256.of(5),
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(1)
        Assertions.assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should calculate owner value on revert burn event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ONE

        val ownership = mapOf(
            owner to EthUInt256.of(8),
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomBurnItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(1)
        Assertions.assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.of(9))
    }

    @Test
    fun `should add owner if all burned reverted`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = emptyMap<Address, EthUInt256>()
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomBurnItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = reversedOwnersItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.ownerships.keys).hasSize(1)
        Assertions.assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.TEN)
    }
}
