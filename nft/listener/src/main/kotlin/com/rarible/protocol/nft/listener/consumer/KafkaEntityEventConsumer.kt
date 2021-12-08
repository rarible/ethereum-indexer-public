package com.rarible.protocol.nft.listener.consumer

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.util.getLogTopicPrefix
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.service.EntityEventListener
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import java.time.Duration

class KafkaEntityEventConsumer(
    private val properties: KafkaProperties,
    private val daemonProperties: DaemonWorkerProperties,
    private val meterRegistry: MeterRegistry,
    host: String,
    environment: String,
    blockchain: String,
    private val service: String
) : EntityEventConsumer {

    private val topicPrefix = getLogTopicPrefix(environment, service, blockchain)
    private val clientIdPrefix = "$environment.$host.${java.util.UUID.randomUUID()}.$blockchain"

    override fun start(handler: Map<SubscriberGroup, EntityEventListener>) {
        handler
            .map { consumer(it.key, it.value) }
            .forEach { it.start() }
    }

    private fun consumer(group: SubscriberGroup, listener: EntityEventListener): ConsumerBatchWorker<*> {
        val kafkaConsumer = RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.log-event-consumer.$service.$group",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = ReversedEthereumLogRecord::class.java,
            consumerGroup = group,
            defaultTopic = "$topicPrefix.$group",
            bootstrapServers = properties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            properties = mapOf(
                "max.poll.records" to properties.maxPollRecords.toString(),
                "allow.auto.create.topics" to "false"
            )
        )
        return ConsumerBatchWorker(
            consumer = kafkaConsumer,
            properties = daemonProperties,
            // Block consumer should NOT skip events, so there is we're using endless retry
            retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
            eventHandler = BlockEventHandler(listener),
            meterRegistry = meterRegistry,
            workerName = "log-event-consumer-$group"
        )
    }

    private class BlockEventHandler(
        private val entityEventListener: EntityEventListener
    ) : ConsumerBatchEventHandler<ReversedEthereumLogRecord> {
        override suspend fun handle(event: List<ReversedEthereumLogRecord>) {
            entityEventListener.onEntityEvents(event)
        }
    }
}
