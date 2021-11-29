package com.rarible.protocol.unlockable.api.service

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.LockFormDto
import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.dto.UnlockableTopicProvider
import com.rarible.protocol.unlockable.configuration.LockEventProducerProperties
import com.rarible.protocol.unlockable.test.LockTestDataFactory
import io.mockk.coEvery
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@MongoTest
@KafkaTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "application.environment=test"
    ]
)
@ActiveProfiles("test")
internal class LockServiceIt {

    @Autowired
    private lateinit var properties: LockEventProducerProperties

    @Autowired
    private lateinit var blockchain: Blockchain

    @Autowired
    private lateinit var lockService: LockService

    @MockkBean
    private lateinit var nftClientService: NftClientService

    @Test
    fun `lock events`() = runBlocking {
        val consumer = createConsumer()
        val events = Collections.synchronizedList(ArrayList<KafkaMessage<UnlockableEventDto>>())
        val job: Deferred<Unit> = async {
            consumer.receive().collect { events.add(it) }
        }

        val itemId = "0x67abb7fc7a1b120ec9e0ad741336ceaff051485a:1"
        val testData = LockTestDataFactory.randomLockTestData(itemId)
        val lockForm = LockFormDto(testData.getItemLockSignature(), testData.lockContent)

        // LOCK_CREATED event
        lockService.createLock(testData.nftItem, lockForm)

        coEvery { nftClientService.hasItem(any(), any(), any()) } returns true

        // LOCK_UNLOCKED event
        lockService.getContent(testData.nftItem, lockForm.signature)

        Wait.waitAssert {
            Assertions.assertThat(events)
                .hasSize(2)
                .satisfies {
                    Assertions.assertThat(it[0].value)
                        .hasFieldOrPropertyWithValue(UnlockableEventDto::itemId.name, itemId)
                        .hasFieldOrPropertyWithValue(
                            UnlockableEventDto::type.name,
                            UnlockableEventDto.Type.LOCK_CREATED
                        )
                    Assertions.assertThat(it[1].value)
                        .hasFieldOrPropertyWithValue(UnlockableEventDto::itemId.name, itemId)
                        .hasFieldOrPropertyWithValue(
                            UnlockableEventDto::type.name,
                            UnlockableEventDto.Type.LOCK_UNLOCKED
                        )
                }
        }
        job.cancel()
    }

    private fun createConsumer(): RaribleKafkaConsumer<UnlockableEventDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer",
            consumerGroup = "test-group",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = UnlockableEventDto::class.java,
            defaultTopic = UnlockableTopicProvider.getTopic(properties.environment, blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }
}
