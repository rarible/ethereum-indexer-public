package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.nft.core.converters.dto.NftActivityConverter
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class OnNftItemLogEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) {
    private val topics: List<Word> = ItemType.TRANSFER.topic.toList()

    suspend fun onLogEvent(logEvent: LogRecordEvent<ReversedEthereumLogRecord>) {
        if (logEvent.reverted.not() && logEvent.record.log.topic in topics) {
            val activity = NftActivityConverter.convert(logEvent.record)
            if (activity != null) eventPublisher.publish(activity)
        }
    }
}
