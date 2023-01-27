package com.rarible.protocol.order.core.service.block.nonce

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.service.ChangeCounterListener
import com.rarible.protocol.order.core.service.ContractsProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("nonce-event-subscriber")
class NonceEventSubscriber(
    private val contractProvider: ContractsProvider,
    private val changeNonceListener: ChangeCounterListener,
) : EntityEventsSubscriber {

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        val protocols = contractProvider.seaportV1() + contractProvider.looksrareV1()

        val nonceEvents = events
            .map { event -> event.record.asEthereumLogRecord() }
            .filter { record -> record.address in protocols }
            .filter { record -> record.data is ChangeNonceHistory }

        val nonceHistories = nonceEvents
            .map { log -> log.data as ChangeNonceHistory }
            .distinct()

        nonceHistories.forEach { history ->
            changeNonceListener.onNewMakerNonce(
                history.source.toPlatform(),
                history.maker,
                history.newNonce.value.toLong(),
                history.date
            )
        }
    }
}

