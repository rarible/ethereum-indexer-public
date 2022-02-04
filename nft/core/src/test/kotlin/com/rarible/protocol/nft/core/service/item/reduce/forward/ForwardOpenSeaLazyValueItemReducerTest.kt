package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomEthereumLog
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

internal class ForwardOpenSeaLazyValueItemReducerTest {
    private val properties = mockk<NftIndexerProperties> {
        every { openseaLazyMintAddress } returns randomAddress().hex()
    }
    private val forwardOpenSeaLazyValueItemReducer = ForwardOpenSeaLazyValueItemReducer(properties)

    @Test
    fun `should increase value for lazy transfer`() = runBlocking<Unit> {
        val event = createRandomTransferItemEvent().copy(
            from = minter,
            to = randomAddress(),
            value = EthUInt256.TEN,
            log = createRandomEthereumLog().copy(from = Address.apply(properties.openseaLazyMintAddress))
        )
        val item = createRandomItem().copy(
            tokenId = tokenId,
            supply = EthUInt256.ONE,
            creators = emptyList()
        )
        val reducedItem = forwardOpenSeaLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(11))
        assertThat(reducedItem.creators).hasSize(1)
        assertThat(reducedItem.creators[0].account).isEqualTo(minter)
        assertThat(reducedItem.creators[0].value).isEqualTo(10000)
    }

    @Test
    fun `should not increase value for not opensea contract`() = runBlocking<Unit> {
        val event = createRandomTransferItemEvent().copy(
            from = minter,
            to = randomAddress(),
            value = EthUInt256.TEN,
            log = createRandomEthereumLog()
        )
        val item = createRandomItem().copy(
            tokenId = tokenId,
            supply = EthUInt256.ONE,
            creators = emptyList()
        )
        val reducedItem = forwardOpenSeaLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.creators).isEmpty()
    }

    @Test
    fun `should not increase value for simple tokenId`() = runBlocking<Unit> {
        val event = createRandomTransferItemEvent().copy(
            from = minter,
            to = randomAddress(),
            value = EthUInt256.TEN,
            log = createRandomEthereumLog().copy(from = Address.apply(properties.openseaLazyMintAddress))
        )
        val item = createRandomItem().copy(
            tokenId = EthUInt256.TEN,
            supply = EthUInt256.ONE,
            creators = emptyList()
        )
        val reducedItem = forwardOpenSeaLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.creators).isEmpty()
    }

    @Test
    fun `should not increase value if tokenId not from minter name space`() = runBlocking<Unit> {
        val event = createRandomTransferItemEvent().copy(
            from = randomAddress(),
            to = randomAddress(),
            value = EthUInt256.TEN,
            log = createRandomEthereumLog().copy(from = Address.apply(properties.openseaLazyMintAddress))
        )
        val item = createRandomItem().copy(
            tokenId = tokenId,
            supply = EthUInt256.ONE,
            creators = emptyList()
        )
        val reducedItem = forwardOpenSeaLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.creators).isEmpty()
    }

    private companion object {
        val minter = Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6")
        val tokenId = EthUInt256.of(BigInteger("32372326957878872325869669322028881416287194712918919938492792330334129619037"))
    }
}