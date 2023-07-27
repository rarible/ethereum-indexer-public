package com.rarible.protocol.nft.core.converters.model

import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.data.createRandomItemTransfer
import com.rarible.protocol.nft.core.data.createRandomReversedEthereumLogRecord
import com.rarible.protocol.nft.core.misc.addIndexerIn
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitLessThanOffset
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

internal class OwnershipEventConverterTest {

    private val itemUpdateService = mockk<ItemUpdateService> {
        coEvery { get(any<ItemId>()) } returns null
    }
    private val converter = OwnershipEventConverter(itemUpdateService)
    private val timeDelta = TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS)

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
    fun `should convert mint transfer`(transfer: ItemTransfer) = runBlocking<Unit> {
        val logRecord = log(transfer)
        val mintEvent = converter.convert(logRecord)

        assertThat(mintEvent).hasSize(1)
        val transferToEvent = mintEvent.single()
        assertThat(transferToEvent).isInstanceOf(OwnershipEvent.TransferToEvent::class.java)

        with(transferToEvent as OwnershipEvent.TransferToEvent) {
            assertThat(from).isEqualTo(transfer.from)
            assertThat(value).isEqualTo(transfer.value)
            assertThat(entityId).isEqualTo(OwnershipId(transfer.token, transfer.tokenId, transfer.owner).stringValue)
        }
    }

    @Test
    fun `should convert burn transfer`() = runBlocking<Unit> {
        val transfer = createRandomItemTransfer().copy(
            owner = Address.ZERO()
        )
        val logRecord = log(transfer)
        val burnEvent = converter.convert(logRecord)

        assertThat(burnEvent).hasSize(1)
        val transferFromEvent = burnEvent.single()
        assertThat(transferFromEvent).isInstanceOf(OwnershipEvent.TransferFromEvent::class.java)

        with(transferFromEvent as OwnershipEvent.TransferFromEvent) {
            assertThat(to).isEqualTo(transfer.owner)
            assertThat(value).isEqualTo(transfer.value)
            assertThat(entityId).isEqualTo(OwnershipId(transfer.token, transfer.tokenId, transfer.from).stringValue)
        }
    }

    @Test
    fun `convert - with time marks`() = runBlocking<Unit> {
        val blockchainDate = nowMillis().minusSeconds(1000)
        val transfer = createRandomItemTransfer().copy(owner = Address.ZERO())
        val marks = EventTimeMarks("blockchain")
            .add("source", blockchainDate)
            .addIndexerIn()

        val logRecord = log(transfer)
        val eventTimeMarks = converter.convert(logRecord, marks)[0].eventTimeMarks!!

        assertThat(eventTimeMarks.marks[0].name).isEqualTo("source")
        assertThat(eventTimeMarks.marks[0].date).isEqualTo(blockchainDate)
        assertThat(eventTimeMarks.marks[1].name).isEqualTo("indexer-in_nft")
        assertThat(eventTimeMarks.marks[1].date).isCloseTo(nowMillis(), timeDelta)
    }

    @Test
    fun `should convert transfer`() = runBlocking<Unit> {
        val transfer = createRandomItemTransfer()
        val logRecord = log(transfer)
        val event = converter.convert(logRecord)

        assertThat(event).hasSize(2)
        val transferToEvent = event.first { it is OwnershipEvent.TransferToEvent }
        val transferFromEvent = event.first { it is OwnershipEvent.TransferFromEvent }

        with(transferFromEvent as OwnershipEvent.TransferFromEvent) {
            assertThat(to).isEqualTo(transfer.owner)
            assertThat(value).isEqualTo(transfer.value)
            assertThat(entityId).isEqualTo(OwnershipId(transfer.token, transfer.tokenId, transfer.from).stringValue)
        }
        with(transferToEvent as OwnershipEvent.TransferToEvent) {
            assertThat(from).isEqualTo(transfer.from)
            assertThat(value).isEqualTo(transfer.value)
            assertThat(entityId).isEqualTo(OwnershipId(transfer.token, transfer.tokenId, transfer.owner).stringValue)
        }
    }

    private fun log(event: ItemTransfer) = createRandomReversedEthereumLogRecord(event)
}
