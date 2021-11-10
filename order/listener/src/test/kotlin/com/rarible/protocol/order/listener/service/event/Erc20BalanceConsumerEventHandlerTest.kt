package com.rarible.protocol.order.listener.service.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory
import java.time.Duration
import java.util.*

@IntegrationTest
internal class Erc20BalanceConsumerEventHandlerTest : AbstractIntegrationTest() {
    @Test
    fun handleErc20Event() = runBlocking {
        val producer = RaribleKafkaProducer(
            clientId = "erc20",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = Erc20BalanceEventDto::class.java,
            defaultTopic = Erc20BalanceEventTopicProvider.getTopic(application.name, orderIndexerProperties.blockchain.value),
            bootstrapServers = orderIndexerProperties.kafkaReplicaSet
        )

        val erc20 = AddressFactory.create()

        val orderVersion = createOrderVersion().copy(
            make = Asset(Erc20AssetType(erc20), EthUInt256.TEN),
            take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        )
        orderUpdateService.save(orderVersion)

        val event = Erc20BalanceUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            balanceId = UUID.randomUUID().toString(),
            balance =  Erc20BalanceDto(
                contract = erc20,
                owner = orderVersion.maker,
                balance = EthUInt256.TEN.value
            )
        )

        val sendJob = async {
            val message = KafkaMessage(
                key = "test",
                id = UUID.randomUUID().toString(),
                value = event as Erc20BalanceEventDto,
                headers = emptyMap()
            )
            while (true) {
                producer.send(message)
                delay(Duration.ofMillis(10).toMillis())
            }
        }

        Wait.waitAssert(Duration.ofSeconds(10)) {
            val updatedOrder = orderRepository.findById(orderVersion.hash)
            assertThat(updatedOrder?.makeStock).isEqualTo(EthUInt256.TEN)
        }
        sendJob.cancel()
    }
}
