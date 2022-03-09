package com.rarible.protocol.nft.api.e2e

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CopyOnWriteArrayList

abstract class EventAwareBaseTest : SpringContainerBaseTest() {

    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    private lateinit var properties: NftIndexerProperties

    private lateinit var itemEventConsumer: RaribleKafkaConsumer<NftItemEventDto>

    private lateinit var consumingJobs: List<Job>

    protected val itemEvents = CopyOnWriteArrayList<NftItemEventDto>()

    private fun createItemEventConsumer(): RaribleKafkaConsumer<NftItemEventDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer-item-event",
            consumerGroup = "test-group-item-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @BeforeEach
    fun setUpEventConsumers() {
        itemEventConsumer = createItemEventConsumer()
        @Suppress("EXPERIMENTAL_API_USAGE")
        consumingJobs = listOf(
            GlobalScope.launch {
                itemEventConsumer.receiveAutoAck().collect {
                    itemEvents += it.value
                }
            }
        )
    }

    @AfterEach
    fun stopConsumers() = runBlocking {
        consumingJobs.forEach { it.cancelAndJoin() }
        itemEvents.clear()
    }
}
