package com.rarible.protocol.nft.listener.consumer

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.util.getLogTopicPrefix
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.nft.core.service.EntityEventListener
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import scalether.domain.Address
import java.time.Duration

class KafkaEntityEventConsumer(
    private val properties: KafkaProperties,
    private val daemonProperties: DaemonWorkerProperties,
    private val meterRegistry: MeterRegistry,
    private val ignoreContracts: Set<Address>,
    host: String,
    environment: String,
    blockchain: String,
    private val service: String,
    private val workerCount: Int
) : AutoCloseable {

    private val topicPrefix = getLogTopicPrefix(environment, service, blockchain)
    private val clientIdPrefix = "$environment.$host.${java.util.UUID.randomUUID()}.$blockchain"
    private val batchedConsumerWorkers = arrayListOf<ConsumerWorkerHolder<*>>()

    fun start(entityEventListeners: List<EntityEventListener>) {
        batchedConsumerWorkers += entityEventListeners
            .map { consumer(it) }
            .onEach { consumer -> consumer.start() }
    }

    override fun close() {
        batchedConsumerWorkers.forEach { consumer -> consumer.close() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun consumer(listener: EntityEventListener): ConsumerWorkerHolder<EthereumLogRecordEvent> {
        val workers = (1..workerCount).map { index ->
            val consumerGroup = listener.id
            val kafkaConsumer = RaribleKafkaConsumer(
                clientId = "$clientIdPrefix.log-event-consumer.$service.${listener.id}-$index",
                valueDeserializerClass = JsonDeserializer::class.java,
                valueClass = EthereumLogRecordEvent::class.java,
                consumerGroup = consumerGroup,
                defaultTopic = "$topicPrefix.${listener.subscriberGroup}",
                bootstrapServers = properties.brokerReplicaSet,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                autoCreateTopic = false
            )

            ConsumerWorker(
                consumer = kafkaConsumer,
                properties = daemonProperties,
                // Block consumer should NOT skip events, so there is we're using endless retry
                retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                eventHandler = BlockEventHandler(listener, ignoreContracts),
                meterRegistry = meterRegistry,
                workerName = "log-event-consumer-${listener.id}-$index"
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    private class BlockEventHandler(
        private val entityEventListener: EntityEventListener,
        private val ignoreContracts: Set<Address>,
    ) : ConsumerEventHandler<EthereumLogRecordEvent> {
        override suspend fun handle(event: EthereumLogRecordEvent) {
            if (!ignoreContracts.contains(event.record.address)) {
                entityEventListener.onEntityEvents(listOf(
                    LogRecordEvent(
                        record = event.record,
                        reverted = event.reverted
                    )
                ))
            }
        }
    }

    private data class EthereumLogRecordEvent(
        val record: ReversedEthereumLogRecord,
        val reverted: Boolean
    )
}
