package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.wait.Wait
import com.rarible.opensea.subscriber.OpenseaTopicProvider
import com.rarible.opensea.subscriber.model.OpenseaEvent
import com.rarible.opensea.subscriber.model.OpenseaEventType
import com.rarible.opensea.subscriber.model.OpenseaItemCancelled
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@IntegrationTest
class OpenseaEventHandlerTest : AbstractIntegrationTest() {

    @Test
    fun `check order cancellation`() = runBlocking {

        val order = createOrder().copy(status = OrderStatus.ACTIVE)
        orderVersionRepository.save(order.toOrderVersion()).awaitSingle()
        orderRepository.save(order)

        val producer = RaribleKafkaProducer(
            clientId = UUID.randomUUID().toString(),
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = OpenseaEvent::class.java,
            defaultTopic = OpenseaTopicProvider.getEventTopic("e2e", "ethereum"),
            bootstrapServers = orderIndexerProperties.kafkaReplicaSet
        )
        producer.send(
            KafkaMessage(
                key = "",
                value = OpenseaEvent(
                    eventId = "",
                    event = OpenseaEventType.ITEM_CANCELLED,
                    payload = OpenseaItemCancelled(
                        orderHash = order.id.hash.prefixed(),
                        eventTimestamp = Instant.now(),
                        maker = null
                    )
                ),
                headers = mapOf(),
                id = ""
            )
        ).ensureSuccess()

        Wait.waitAssert {
            val updatedOrder = orderRepository.findById(order.hash)
            assertThat(updatedOrder?.status).isEqualTo(OrderStatus.CANCELLED)
        }
    }
}
