package com.rarible.protocol.nft.core.converters.model

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomEthereumLog
import com.rarible.protocol.nft.core.data.createRandomItemTransfer
import com.rarible.protocol.nft.core.data.createRandomReversedEthereumLogRecord
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.math.BigInteger
import java.util.stream.Stream

internal class ItemEventConverterTest {
    private val openSeaLazy = randomAddress()

    private val converter = ItemEventConverter(
        mockk {
            every { openseaLazyMintAddress } returns openSeaLazy.prefixed()
        }
    )

    private companion object {
        @JvmStatic
        fun mint(): Stream<ItemTransfer> = Stream.of(
            createRandomItemTransfer().copy(
                from = Address.ZERO()
            ),
            createRandomItemTransfer().copy(
                owner = Address.ONE(),
                from = Address.ONE(),
                isMint = true
            )
        )
    }

    @ParameterizedTest
    @MethodSource("mint")
    fun `should convert mint event`(transfer: ItemTransfer) {
        val logRecord = createRandomReversedEthereumLogRecord(transfer)
        val mintEvent = converter.convert(logRecord)
        assertThat(mintEvent).isInstanceOf(ItemEvent.ItemMintEvent::class.java)
        with(mintEvent as ItemEvent.ItemMintEvent) {
            assertThat(supply).isEqualTo(transfer.value)
            assertThat(owner).isEqualTo(transfer.owner)
            assertThat(entityId).isEqualTo(ItemId(transfer.token, transfer.tokenId).stringValue)
        }
    }

    @Test
    fun `should convert burn event`() {
        val transfer = createRandomItemTransfer().copy(
            owner = Address.ZERO()
        )
        val logRecord = createRandomReversedEthereumLogRecord(transfer)
        val mintEvent = converter.convert(logRecord)
        assertThat(mintEvent).isInstanceOf(ItemEvent.ItemBurnEvent::class.java)
        with(mintEvent as ItemEvent.ItemBurnEvent) {
            assertThat(supply).isEqualTo(transfer.value)
            assertThat(owner).isEqualTo(transfer.from)
            assertThat(entityId).isEqualTo(ItemId(transfer.token, transfer.tokenId).stringValue)
        }
    }

    @Test
    fun `should convert null of zero value for owner and from`() {
        val transfer = createRandomItemTransfer().copy(
            owner = Address.ZERO(),
            from = Address.ZERO()
        )
        val logRecord = createRandomReversedEthereumLogRecord(transfer)
        val mintEvent = converter.convert(logRecord)
        assertThat(mintEvent).isNull()
    }

    @Test
    fun `should not convert transfer event`() {
        val transfer = createRandomItemTransfer()
        val logRecord = createRandomReversedEthereumLogRecord(transfer)
        val openSeaLazyMintEvent = converter.convert(logRecord)
        assertThat(openSeaLazyMintEvent).isNull()
    }

    @Test
    fun `should convert to lazy open sea mint`() {
        val transfer = createRandomItemTransfer().copy(
            tokenId = EthUInt256.of(BigInteger("32372326957878872325869669322028881416287194712918919938492792330334129619037"))
        )
        val log = createRandomEthereumLog().copy(
            address = openSeaLazy
        )
        val logRecord = createRandomReversedEthereumLogRecord(transfer).withLog(log)

        val openSeaLazyMintEvent = converter.convert(logRecord)
        assertThat(openSeaLazyMintEvent).isInstanceOf(ItemEvent.OpenSeaLazyItemMintEvent::class.java)
        with(openSeaLazyMintEvent as ItemEvent.OpenSeaLazyItemMintEvent) {
            assertThat(supply).isEqualTo(transfer.value)
            assertThat(from).isEqualTo(transfer.from)
            assertThat(entityId).isEqualTo(ItemId(transfer.token, transfer.tokenId).stringValue)
        }
    }

    @Test
    fun `should not convert to lazy open sea mint if it is simple tokenId`() {
        val transfer = createRandomItemTransfer().copy(
            tokenId = EthUInt256.ONE
        )
        val log = createRandomEthereumLog().copy(
            address = openSeaLazy
        )
        val logRecord = createRandomReversedEthereumLogRecord(transfer).withLog(log)

        val openSeaLazyMintEvent = converter.convert(logRecord)
        assertThat(openSeaLazyMintEvent).isNull()
    }
}