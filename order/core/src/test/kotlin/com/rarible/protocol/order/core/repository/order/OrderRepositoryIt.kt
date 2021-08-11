package com.rarible.protocol.order.core.repository.order

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderDto
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.core.convert.ConversionService
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [RepositoryConfiguration::class])
internal class OrderRepositoryIt {

    @MockkBean
    private lateinit var conversionService: ConversionService

    @MockkBean
    private lateinit var publisher: ProtocolOrderPublisher

    @Autowired
    private lateinit var mongo: ReactiveMongoTemplate

    // Wrapped by NotifiableOrderRepositoryDecorator
    @Autowired
    private lateinit var orderRepository: OrderRepository

    // Simple Order repository
    private lateinit var delegate: OrderRepository

    @BeforeEach
    fun beforeEach() {
        delegate = MongoOrderRepository(mongo)
        clearMocks(conversionService, publisher)
    }

    @Test
    fun `should save order and publish change event`() = runBlocking<Unit> {
        val order = createOrder().copy(makeStock = EthUInt256.of(6))
        delegate.save(order, null)

        val updatedOrder = delegate.findById(order.hash)!!
            .withMakeBalance(EthUInt256.Companion.of(4), EthUInt256.ZERO)

        every { conversionService.convert(any(), eq(OrderDto::class.java)) } returns createOrderDto()
        coEvery { publisher.publish(any<OrderEventDto>()) } returns Unit

        orderRepository.save(updatedOrder, order)
        coVerify { publisher.publish(any<OrderEventDto>()) }
    }

    @Test
    fun `should not save order and not publish order change event`() = runBlocking<Unit> {
        val order = createOrder().copy(makeStock = EthUInt256.of(6))
        delegate.save(order, null)

        orderRepository.save(order, order)
        coVerify(exactly = 0) { publisher.publish(any<OrderEventDto>()) }
    }

    @Test
    fun `test order raw format`() = runBlocking<Unit> {
        val order = createOrder()

        delegate.save(order, null)

        val document = mongo.findById(
            order.hash,
            Document::class.java,
            MongoOrderRepository.COLLECTION
        ).block()

        assertEquals(
            order.maker,
            Address.apply(document.getString(Order::maker.name))
        )
        assertEquals(
            order.createdAt.toEpochMilli(),
            document.getDate(Order::createdAt.name).time
        )
        assertEquals(
            order.fill,
            EthUInt256.Companion.of(document.getString(Order::fill.name))
        )
    }

    @Test
    fun `should remove order`() = runBlocking {
        val hash = delegate.save(createOrder()).hash

        val savedOrder = delegate.findById(hash)
        assertThat(savedOrder).isNotNull

        orderRepository.remove(hash)

        val removedOrder = delegate.findById(hash)
        assertThat(removedOrder).isNull()
    }
}

