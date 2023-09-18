package com.rarible.protocol.erc20.core.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.ethereum.monitoring.EventCountMetrics.EventType
import com.rarible.ethereum.monitoring.EventCountMetrics.Stage
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties

class Erc20EventPublisher(
    private val erc20EventProducer: RaribleKafkaProducer<Erc20BalanceEventDto>,
    private val properties: Erc20IndexerProperties,
    private val eventCountMetrics: EventCountMetrics
) {

    private val erc20EventHeaders = mapOf("protocol.erc20.event.version" to Erc20BalanceEventTopicProvider.VERSION)

    suspend fun publish(event: Erc20BalanceEventDto) = withMetric(EventType.ERC20) {
        val message = KafkaMessage(
            key = event.balanceId,
            value = event,
            headers = erc20EventHeaders,
            id = event.eventId
        )
        erc20EventProducer.send(message).ensureSuccess()
    }

    private suspend fun withMetric(type: EventType, delegate: suspend () -> Unit) {
        try {
            eventCountMetrics.eventSent(Stage.INDEXER_INTERNAL, properties.blockchain.value, type)
            delegate()
        } catch (e: Exception) {
            eventCountMetrics.eventSent(Stage.INDEXER_INTERNAL, properties.blockchain.value, type, -1)
            throw e
        }
    }
}
