package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import com.rarible.protocol.nft.core.repository.data.createAddress
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

internal class ForwardOwnersItemReducerTest {
    private val forwardOwnersItemReducer = ForwardOwnersItemReducer()

    @Test
    fun `should calculate correct owners value for transfer`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.of(4)

        val ownership = mapOf(
            from to EthUInt256.of(5),
            to to EthUInt256.of(6)
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(2)
        assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.ownerships[to]).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `remove owner with zero value`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.of(4)

        val ownership = mapOf(
            from to EthUInt256.of(4),
            to to EthUInt256.of(6)
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[to]).isEqualTo(EthUInt256.TEN)
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

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(2)
        assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.of(4))
        assertThat(reducedItem.ownerships[to]).isEqualTo(EthUInt256.of(6))
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

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[from]).isEqualTo(EthUInt256.of(4))
    }

    @Test
    fun `should do nothing if transfer with zero and to add owners`() = runBlocking<Unit> {
        val from = createAddress()
        val to = createAddress()
        val value = EthUInt256.ZERO

        val ownership = emptyMap<Address, EthUInt256>()

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = from, to = to, value = value)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(0)
    }

    @Test
    fun `should add owner to map for mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = emptyMap<Address, EthUInt256>()

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should calculate owner value for mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = mapOf(
            owner to EthUInt256.of(5),
        )

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.of(15))
    }

    @Test
    fun `should do nothing if zero mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ZERO

        val ownership = emptyMap<Address, EthUInt256>()

        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(0)
    }

    @Test
    fun `should not change owner value if zero mint event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ZERO

        val ownership = mapOf(
            owner to EthUInt256.of(5),
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomMintItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should calculate owner value on burn event`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ONE

        val ownership = mapOf(
            owner to EthUInt256.TEN,
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomBurnItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.of(9))
    }

    @Test
    fun `should remove owner if all burned`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = mapOf(
            owner to EthUInt256.TEN,
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomBurnItemEvent().copy(owner = owner, supply = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(0)
    }

    @Test
    fun `should not change owner value if he transfers to self`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.ONE

        val ownership = mapOf(
            owner to EthUInt256.TEN,
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = owner, to = owner, value = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should not change owner value if he transfers to self the same value`() = runBlocking<Unit> {
        val owner = createAddress()
        val supply = EthUInt256.TEN

        val ownership = mapOf(
            owner to EthUInt256.TEN,
        )
        val item = createRandomItem().copy(ownerships = ownership)
        val event = createRandomTransferItemEvent().copy(from = owner, to = owner, value = supply)

        val reducedItem = forwardOwnersItemReducer.reduce(item, event)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.TEN)
    }
}
