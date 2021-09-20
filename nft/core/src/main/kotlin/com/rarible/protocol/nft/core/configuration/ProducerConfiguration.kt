package com.rarible.protocol.nft.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.producer.BatchedConsumerWorker
import com.rarible.protocol.nft.core.producer.ProducerFactory
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.service.item.meta.InternalItemHandler
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProducerConfiguration(
    private val properties: NftIndexerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    private val logger = LoggerFactory.getLogger(ProducerConfiguration::class.java)

    @Bean
    fun nftEventProducerFactory(): ProducerFactory {
        return ProducerFactory(
            kafkaReplicaSet = properties.kafkaReplicaSet,
            blockchain = properties.blockchain,
            environment = applicationEnvironmentInfo.name
        )
    }

    @Bean
    fun protocolNftEventPublisher(producerFactory: ProducerFactory): ProtocolNftEventPublisher {
        return ProtocolNftEventPublisher(
            producerFactory.createItemEventsProducer(),
            producerFactory.createInternalItemEventsProducer(),
            producerFactory.createOwnershipEventsProducer(),
            producerFactory.createItemActivityProducer()
        )
    }

    @Bean
    fun itemMetaExtenderWorker(internalItemHandler: InternalItemHandler): BatchedConsumerWorker<NftItemEventDto> {
        logger.info("Creating batch of ${properties.nftItemMetaExtenderWorkersCount} item meta extender workers")
        val workers = (1..properties.nftItemMetaExtenderWorkersCount).map {
            ConsumerWorker(
                consumer = InternalItemHandler.createInternalItemConsumer(
                    applicationEnvironmentInfo,
                    properties.blockchain,
                    properties.kafkaReplicaSet
                ),
                properties = properties.daemonWorkerProperties,
                eventHandler = internalItemHandler,
                meterRegistry = meterRegistry,
                workerName = "nftItemMetaExtender.$it"
            )
        }
        return BatchedConsumerWorker(workers)
    }

    @Bean
    fun itemMetaExtenderWorkerStarter(itemMetaExtenderWorker: BatchedConsumerWorker<NftItemEventDto>): CommandLineRunner =
        CommandLineRunner { itemMetaExtenderWorker.start() }
}
