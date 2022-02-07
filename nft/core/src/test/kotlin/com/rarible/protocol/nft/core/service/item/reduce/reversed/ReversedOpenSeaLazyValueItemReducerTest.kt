package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomEthereumLog
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

internal class ReversedOpenSeaLazyValueItemReducerTest {
    private val properties = mockk<NftIndexerProperties> {
        every { openseaLazyMintAddress } returns randomAddress().hex()
    }
    private val reversedOpenSeaLazyValueItemReducer = ReversedOpenSeaLazyValueItemReducer(properties)

    @Test
    fun `should revert value for lazy transfer`() = runBlocking<Unit> {
        val event = createRandomTransferItemEvent().copy(
            from = minter,
            to = randomAddress(),
            value = EthUInt256.TEN,
            log = createRandomEthereumLog().copy(address = Address.apply(properties.openseaLazyMintAddress))
        )
        val item = createRandomItem().copy(
            tokenId = tokenId,
            supply = EthUInt256.of(11),
            creators = emptyList()
        )
        val reducedItem = reversedOpenSeaLazyValueItemReducer.reduce(item, event)
        Assertions.assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
    }

    private companion object {
        val minter = Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6")
        val tokenId = EthUInt256.of(BigInteger("32372326957878872325869669322028881416287194712918919938492792330334129619037"))
    }
}
