package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.dto.NftActivityConverter
import com.rarible.protocol.nft.core.data.randomReversedLogRecord
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.repository.data.createItemHistory
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OnNftItemLogEventListenerTest {

    val publisher: ProtocolNftEventPublisher = mockk()
    val converter = NftActivityConverter(NftIndexerProperties.ContractAddresses())

    val listener = OnNftItemLogEventListener(publisher, converter)

    @BeforeEach
    fun beforeEach() {
        clearMocks(publisher)
        coEvery { publisher.publish(any<List<Pair<NftActivityDto, EventTimeMarks>>>()) } returns Unit
    }

    @Test
    fun `on log event`() = runBlocking<Unit> {
        val record = randomReversedLogRecord(createItemHistory())
            .copy(topic = TransferEvent.id())

        listener.onLogEvents(listOf(LogRecordEvent(record, false, EventTimeMarks("test"))))

        coVerify(exactly = 1) { publisher.publish(any<List<Pair<NftActivityDto, EventTimeMarks>>>()) }
    }

    @Test
    fun `on log event - not confirmed`() = runBlocking<Unit> {
        val record = randomReversedLogRecord(createItemHistory()).copy(
            status = EthereumBlockStatus.REVERTED,
            topic = TransferEvent.id()
        )

        val event = LogRecordEvent(record, false, EventTimeMarks("test"))

        listener.onLogEvents(listOf(event))

        coVerify(exactly = 0) { publisher.publish(any<List<Pair<NftActivityDto, EventTimeMarks>>>()) }
    }

    @Test
    fun `on log event - reverted`() = runBlocking<Unit> {
        val record = randomReversedLogRecord(createItemHistory())
            .copy(topic = TransferEvent.id())

        listener.onLogEvents(listOf(LogRecordEvent(record, true, EventTimeMarks("test"))))

        coVerify(exactly = 1) { publisher.publish(any<List<Pair<NftActivityDto, EventTimeMarks>>>()) }
    }

    @Test
    fun `on log event - not in topic set`() = runBlocking<Unit> {
        val record = randomReversedLogRecord(createItemHistory())

        listener.onLogEvents(listOf(LogRecordEvent(record, true, EventTimeMarks("test"))))

        coVerify(exactly = 0) { publisher.publish(any<List<Pair<NftActivityDto, EventTimeMarks>>>()) }
    }
}
