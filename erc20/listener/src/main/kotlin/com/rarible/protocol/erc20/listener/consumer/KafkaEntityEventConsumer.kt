package com.rarible.protocol.erc20.listener.consumer

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecordEvent
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.util.getLogTopicPrefix
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.erc20.core.misc.addIn
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
            ConsumerBatchWorker(
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
    ) : ConsumerBatchEventHandler<EthereumLogRecordEvent> {
        override suspend fun handle(event: List<EthereumLogRecordEvent>) {
            val filteredEvents = event.filter {
                ignoreContracts.contains(it.record.address).not()
            }
            entityEventListener.onEntityEvents(filteredEvents.map {
                LogRecordEvent(
                    record = it.record,
                    reverted = it.reverted,
                    eventTimeMarks = it.eventTimeMarks.addIn()
                )
            })
        }
    }

}
