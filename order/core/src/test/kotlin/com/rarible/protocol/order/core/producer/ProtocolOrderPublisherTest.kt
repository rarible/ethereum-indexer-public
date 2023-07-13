package com.rarible.protocol.order.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLooksrareOrderDto
import com.rarible.protocol.order.core.data.createOrderDto
import com.rarible.protocol.order.core.data.createSeaportOrderDto
import com.rarible.protocol.order.core.data.createX2Y2OrderDto
import com.rarible.protocol.order.core.data.randomOrderEventDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ProtocolOrderPublisherTest {
    private val ordersPriceUpdateEventProducer = mockk<RaribleKafkaProducer<NftOrdersPriceUpdateEventDto>>()
    private val orderActivityProducer = mockk<RaribleKafkaProducer<EthActivityEventDto>>()
    private val publishProperties = mockk<OrderIndexerProperties.PublishProperties>()
    private val orderEventProducer = mockk<RaribleKafkaProducer<OrderEventDto>> {
        coEvery { send(any<KafkaMessage<OrderEventDto>>()) } returns KafkaSendResult.Success("ok")
    }
    private val publisher = ProtocolOrderPublisher(
        orderActivityProducer,
        orderEventProducer,
        ordersPriceUpdateEventProducer,
        publishProperties
    )

    private companion object {
        @JvmStatic
        fun publishFlags(): Stream<Boolean> = Stream.of(true, false)
    }

    @ParameterizedTest
    @MethodSource("publishFlags")
    fun `should publish x2y2 orders`(needPublish: Boolean) = runBlocking {
        every { publishProperties.publishX2Y2Orders } returns needPublish
        checkPublish(createX2Y2OrderDto(), needPublish)
    }

    @ParameterizedTest
    @MethodSource("publishFlags")
    fun `should publish looksrare orders`(needPublish: Boolean) = runBlocking {
        every { publishProperties.publishLooksrareOrders } returns needPublish
        checkPublish(createLooksrareOrderDto(), needPublish)
    }

    @ParameterizedTest
    @MethodSource("publishFlags")
    fun `should publish seaport orders`(needPublish: Boolean) = runBlocking {
        every { publishProperties.publishSeaportOrders } returns needPublish
        checkPublish(createSeaportOrderDto(), needPublish)
    }

    @Test
    fun `should publish rarible`() = runBlocking {
        checkPublish(createOrderDto(), true)
    }

    private suspend fun checkPublish(order: OrderDto, needPublish: Boolean) {
        val event = randomOrderEventDto().copy(order = order)
        publisher.publish(event)
        coVerify(exactly = if (needPublish) 1 else 0) { orderEventProducer.send(any<KafkaMessage<OrderEventDto>>()) }
    }
}