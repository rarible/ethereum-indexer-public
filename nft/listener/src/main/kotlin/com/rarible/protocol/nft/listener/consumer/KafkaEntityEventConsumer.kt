package com.rarible.protocol.nft.listener.consumer

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecordEvent
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.util.getLogTopicPrefix
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaEventHandler
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import scalether.domain.Address

class KafkaEntityEventConsumer(
    private val properties: KafkaProperties,
    private val ignoreContracts: Set<Address>,
    private val host: String,
    private val environment: String,
    blockchain: String,
    private val service: String,
    private val workerCount: Int,
    private val batchSize: Int,
) : AutoCloseable {

    private val topicPrefix = getLogTopicPrefix(environment, service, blockchain, "log")
    private val clientIdPrefix = "$environment.$host.${java.util.UUID.randomUUID()}.$blockchain"
    private val batchedConsumerWorkers = arrayListOf<RaribleKafkaConsumerWorker<*>>()

    fun start(entityEventListeners: List<EntityEventListener>) {
        batchedConsumerWorkers += entityEventListeners
            .map { consumer(it) }
            .onEach { consumer -> consumer.start() }
    }

    override fun close() {
        batchedConsumerWorkers.forEach { consumer -> consumer.close() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun consumer(listener: EntityEventListener): RaribleKafkaConsumerWorker<EthereumLogRecordEvent> {
        val factory = RaribleKafkaConsumerFactory(
            env = environment,
            host = host
        )
        val settings = RaribleKafkaConsumerSettings<EthereumLogRecordEvent>(
            hosts = properties.brokerReplicaSet,
            topic = "$topicPrefix.${listener.subscriberGroup}",
            group = listener.id,
            concurrency = workerCount,
            batchSize = batchSize,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = EthereumLogRecordEvent::class.java,
        )
        return factory.createWorker(
            settings = settings,
            handler = BlockEventHandler(listener, ignoreContracts)
        )
    }

    private class BlockEventHandler(
        private val entityEventListener: EntityEventListener,
        private val ignoreContracts: Set<Address>,
    ) : RaribleKafkaEventHandler<EthereumLogRecordEvent> {
        override suspend fun handle(event: EthereumLogRecordEvent) {
            val filteredEvents = listOf(event).filter {
                ignoreContracts.contains(it.record.address).not()
            }
            entityEventListener.onEntityEvents(filteredEvents.map {
                LogRecordEvent(
                    record = it.record,
                    reverted = it.reverted,
                    eventTimeMarks = it.eventTimeMarks
                )
            })
        }
    }
}
