package com.rarible.protocol.erc20.core.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider

class Erc20EventPublisher(
    private val erc20EventProducer: RaribleKafkaProducer<Erc20BalanceEventDto>
) {

    private val erc20EventHeaders = mapOf("protocol.erc20.event.version" to Erc20BalanceEventTopicProvider.VERSION)

    suspend fun publish(event: Erc20BalanceEventDto) {
        val message = KafkaMessage(
            key = event.balanceId,
            value = event,
            headers = erc20EventHeaders,
            id = event.eventId
        )
        erc20EventProducer.send(message).ensureSuccess()
    }
}
