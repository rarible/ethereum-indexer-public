package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.nft.core.converters.dto.NftActivityConverter
import com.rarible.protocol.nft.core.misc.asEthereumLogRecord
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OnNftItemLogEventListener(
    private val eventPublisher: ProtocolNftEventPublisher,
    private val nftActivityConverter: NftActivityConverter
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val topics: Set<Word> = ItemType.TRANSFER.topic.toSet()

    suspend fun onLogEvents(logEvents: List<LogRecordEvent>) {
        val events = logEvents.mapNotNull { toDto(it) }

        if (events.isNotEmpty()) {
            eventPublisher.publish(events)
        }
    }

    private fun toDto(logEvent: LogRecordEvent): Pair<NftActivityDto, EventTimeMarks>? {
        try {
            val record = logEvent.record.asEthereumLogRecord()
            if (record.status != EthereumBlockStatus.CONFIRMED || record.log.topic !in topics) {
                return null
            }

            val dto = nftActivityConverter.convert(record, logEvent.reverted) ?: return null
            return dto to logEvent.eventTimeMarks
        } catch (ex: Exception) {
            logger.error("Error on log event $logEvent", ex)
            throw ex
        }
    }
}
