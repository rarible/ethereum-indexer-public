package com.rarible.protocol.order.core.repository.order

import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.model.Order
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address

@MongoTest
@DataMongoTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [RepositoryConfiguration::class])
internal class OrderRepositoryIt {

    @Autowired
    private lateinit var mongo: ReactiveMongoTemplate

    @Autowired
    private lateinit var orderRepository: OrderRepository

    // Simple Order repository
    private lateinit var delegate: OrderRepository

    @BeforeEach
    fun beforeEach() {
        delegate = MongoOrderRepository(mongo)
    }

    @Test
    fun `test order raw format`() = runBlocking<Unit> {
        val order = createOrder()

        delegate.save(order)

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

