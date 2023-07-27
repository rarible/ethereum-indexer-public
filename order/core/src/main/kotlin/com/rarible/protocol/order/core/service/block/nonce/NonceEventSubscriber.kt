package com.rarible.protocol.order.core.service.block.nonce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.service.ChangeCounterListener
import com.rarible.protocol.order.core.service.ContractsProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("nonce-event-subscriber")
class NonceEventSubscriber(
    contractProvider: ContractsProvider,
    private val changeNonceListener: ChangeCounterListener,
) : LogRecordEventSubscriber {

    private val protocols = contractProvider.seaportV1() +
        contractProvider.looksrareV1() +
        contractProvider.blurV1()

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {

        val nonceHistories = events
            .mapNotNull { event ->
                val record = event.record.asEthereumLogRecord()
                val data = record.data
                if (record.address in protocols && data is ChangeNonceHistory) {
                    data to event
                } else {
                    null
                }
            }.distinctBy { it.first }

        nonceHistories.forEach { pair ->
            val history = pair.first
            val event = pair.second
            val eventTimeMarks = event.eventTimeMarks.addIndexerIn()
            changeNonceListener.onNewMakerNonce(
                history.source.toPlatform(),
                history.maker,
                history.newNonce.value,
                history.date,
                eventTimeMarks
            )
        }
    }
}
