package com.rarible.protocol.order.listener.service.event

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.listener.data.createNftOwnershipDto
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.time.Duration
import java.util.*

@IntegrationTest
internal class NftOwnershipConsumerEventHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Test
    fun handleErc20Event() = runBlocking {
        val producer = RaribleKafkaProducer(
            clientId = "ownership",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(
                application.name,
                orderIndexerProperties.blockchain.value
            ),
            bootstrapServers = orderIndexerProperties.kafkaReplicaSet
        )

        val collection = AddressFactory.create()
        val toneId = EthUInt256.TEN
        val orderVersion = createOrderVersion().copy(
            make = Asset(Erc1155AssetType(collection, toneId), EthUInt256.TEN),
            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
        )
        orderUpdateService.save(orderVersion)

        val event = NftOwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = UUID.randomUUID().toString(),
            ownership = createNftOwnershipDto().copy(
                id = UUID.randomUUID().toString(),
                owner = orderVersion.maker,
                contract = collection,
                tokenId = toneId.value,
                value = EthUInt256.of(3).value
            )
        )

        val sendJob = async {
            val message = KafkaMessage(
                key = "test",
                id = UUID.randomUUID().toString(),
                value = event as NftOwnershipEventDto,
                headers = emptyMap()
            )
            while (true) {
                producer.send(message)
                delay(Duration.ofMillis(10).toMillis())
            }
        }

        Wait.waitAssert(Duration.ofSeconds(10)) {
            val updatedOrder = orderRepository.findById(orderVersion.hash)
            Assertions.assertThat(updatedOrder?.makeStock).isEqualTo(EthUInt256.of(3))
        }
        sendJob.cancel()
    }
}
